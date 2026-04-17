# CompileTime — Architecture Document

> Last updated: April 17, 2026
> Status: Milestone 2 complete
> Author: Gaurav

---

## 1. System Overview

CompileTime is a developer productivity tool that detects idle/waiting moments
(builds, installs, docker pulls) and fills them with MCQ quizzes via a native
overlay — keeping developers learning instead of doom-scrolling.

```
┌─────────────────────────────────────────────────────────────┐
│                        USER'S MACHINE                        │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              COMPANION APP (Spring Boot :9999)        │   │
│  │                                                      │   │
│  │  ┌──────────────────┐    ┌────────────────────────┐  │   │
│  │  │  Detection Layer │    │   JavaFX Overlay       │  │   │
│  │  │                  │    │   (always-on-top,      │  │   │
│  │  │  ┌────────────┐  │    │    transparent,        │  │   │
│  │  │  │ PS Hook    │  │    │    bottom-right)       │  │   │
│  │  │  │ hooks.ps1  │  │───▶│                        │  │   │
│  │  │  └────────────┘  │    │  - Question card       │  │   │
│  │  │  ┌────────────┐  │    │  - MCQ options         │  │   │
│  │  │  │ Process    │  │    │  - Timer + XP          │  │   │
│  │  │  │ Monitor    │  │    │  - X close button      │  │   │
│  │  │  │ (CMD fix)  │  │    └────────────────────────┘  │   │
│  │  │  └────────────┘  │                                │   │
│  │  │  ┌────────────┐  │    ┌────────────────────────┐  │   │
│  │  │  │ Command    │  │    │   SQLite Database       │  │   │
│  │  │  │ Classifier │  │    │   ~/.compiletime/       │  │   │
│  │  │  └────────────┘  │    │   data.db               │  │   │
│  │  │  ┌────────────┐  │    └────────────────────────┘  │   │
│  │  │  │ Debounce   │  │                                │   │
│  │  │  │ Service    │  │    ┌────────────────────────┐  │   │
│  │  │  │ (3s)       │  │    │   Question Bank        │  │   │
│  │  │  └────────────┘  │    │   JSON files in        │  │   │
│  │  └──────────────────┘    │   resources/questions/ │  │   │
│  └──────────────────────────┴────────────────────────┘   │   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              SCRIPTS (PowerShell)                     │   │
│  │                                                      │   │
│  │  hooks.ps1          — PSReadLine Enter key hook      │   │
│  │  process-monitor.ps1 — polls Get-Process every 500ms │   │
│  │  start-compiletime.vbs — auto-start on Windows boot  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Detection Layer

### 2.1 PowerShell Hook (`scripts/hooks.ps1`)
Injected into `$PROFILE` by the installer. Uses PSReadLine to intercept
the Enter key — fires BEFORE the command runs.

```powershell
# Pre-command: fires when user presses Enter
Set-PSReadLineKeyHandler -Key Enter -ScriptBlock {
    # get typed command, ping /api/command/start
    [Microsoft.PowerShell.PSConsoleReadLine]::AcceptLine()
}

# Post-command: fires when prompt redraws after command finishes
function prompt {
    # ping /api/command/done
}
```

### 2.2 Process Monitor (`scripts/process-monitor.ps1`)
CMD has no hook API. The process monitor polls `Get-Process` every 500ms
and detects when known build tool executables appear or disappear.

```
Every 500ms:
  Get-Process → filter by known build tool names
  New PID seen?   → POST /api/command/start?cmd=<name>
  Known PID gone? → POST /api/command/done?exit=0
```

Works across ALL terminals automatically — CMD, PowerShell, Git Bash,
Windows Terminal, VS Code integrated terminal.

### 2.3 Command Classifier
Not all commands trigger a quiz. The classifier checks the incoming
command against a configurable list in `application.yml`:

```yaml
compiletime.detection.build-commands:
  - npm run build
  - mvn install
  - docker build
  - git pull
  # ... etc
```

Only matching commands pass through to the debounce timer.

### 2.4 Debounce Service
Prevents fast commands (< 3 seconds) from showing the overlay.
If `/api/command/done` arrives before 3 seconds, the timer is cancelled silently.

---

## 3. Overlay (JavaFX)

A transparent, always-on-top JavaFX window positioned in the bottom-right
corner. Works over any application — not limited to the browser.

**Lifecycle:**
```
BuildService.onTrigger()
    → EventPublisher.publishTrigger()
    → OverlayWindow.show(command, sessionId)
    → JavaFX Platform.runLater() → Stage.show()

User clicks X or answers question
    → OverlayWindow.hide()
    → Stage.hide()
```

**Key design decisions:**
- `StageStyle.TRANSPARENT` — no window chrome
- `setAlwaysOnTop(true)` — floats over everything
- `Platform.startup()` not `Application.launch()` — Spring Boot owns main thread
- Overlay does NOT auto-dismiss when build finishes — user controls it

---

## 4. Data Layer (SQLite)

**Why SQLite and not Postgres?**
CompileTime is a local desktop tool. Each user's data lives entirely on
their own machine. There is no shared server. SQLite is embedded, ships
with the app, requires zero configuration, and handles single-user
local data with ease.

100k DAU = 100k separate SQLite files (one per machine), not 100k
connections to a single database. SQLite is exactly right here.

**Schema:**
```sql
CREATE TABLE build_session (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    command      TEXT NOT NULL,
    started_at   DATETIME NOT NULL,
    ended_at     DATETIME,
    duration_ms  INTEGER,
    exit_code    INTEGER
);

CREATE TABLE xp_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id   INTEGER REFERENCES build_session(id),
    question_id  TEXT NOT NULL,
    correct      BOOLEAN NOT NULL,
    time_ms      INTEGER,
    xp_awarded   INTEGER NOT NULL,
    recorded_at  DATETIME NOT NULL
);
```

---

## 5. Auto-Start (Windows Boot)

```
Install-CompileTime.ps1  (run once)
    │
    ├── Patches $PROFILE with hooks.ps1 source line
    └── Copies start-compiletime.vbs to:
        C:\Users\<user>\AppData\Roaming\Microsoft\Windows\
        Start Menu\Programs\Startup\

On every Windows boot:
    start-compiletime.vbs runs automatically
        ├── Checks if port 9999 is in use (prevents duplicate start)
        ├── Starts companion: mvn spring-boot:run (hidden window)
        └── Starts monitor:   process-monitor.ps1 (hidden window)
```

---

## 6. Tech Stack

| Layer | Technology | Notes |
|-------|------------|-------|
| Companion App | Spring Boot 3 (Java 21) | Background service, REST API |
| Overlay UI | JavaFX | Transparent always-on-top window |
| REST API | Spring MVC | Shell hooks + process monitor ping these |
| Database | SQLite + Spring Data JPA | Per-user local file, zero config |
| PS Hook | PowerShell PSReadLine | Enter key intercept in $PROFILE |
| Process Monitor | PowerShell Get-Process | 500ms polling, covers all terminals |
| Auto-start | VBScript | Silent Windows Startup folder launcher |
| Distribution (future) | jpackage + bundled JRE | Ships as .exe installer |

---

## 7. Key Architectural Decisions

| Decision | Reason |
|----------|--------|
| JavaFX overlay over Chrome Extension | Works over any app, not just browser |
| SQLite over Postgres | Local desktop — each user has their own DB |
| PowerShell PSReadLine hook | Clean pre-command intercept, non-blocking |
| PowerShell process monitor | CMD has no hook API — polling covers all terminals |
| `Platform.startup()` | Spring Boot owns main thread, JavaFX is secondary |
| No WebSocket | Removed when Chrome Extension was dropped — not needed |
| JSON question bank | Simple for MVP; SQLite migration in Milestone 3 |

---

## 8. Future — AI Pipeline (Planned, Milestone 6+)

When the enterprise/social layer lands, a new backend service handles
content generation automatically:

```
Manager drops a blog URL or article
    → Backend fetches content
    → Claude API call:
       "Generate 5 MCQ questions from this, category: infer, difficulty: medium"
    → Structured QnA stored in Postgres under org's question bank
    → Questions surface in team members' overlays during builds
```

Same pipeline will power:
- Book pages → comprehension questions
- Internal docs → onboarding quizzes
- Feed personalization → surface weak areas more often
- Morning content → summarize tech news, make it engaging

This makes question content infinite and zero-maintenance.
See VISION.md for full product roadmap.

---

## 9. Security

- Companion binds to `localhost` only — never exposed to network
- Shell hooks are fire-and-forget, non-blocking, non-destructive
- SQLite stored in user's local app data directory
- No data leaves the machine
