# CompileTime — Project Specification

> Last updated: April 2026  
> Status: Pre-development  
> Author: Gaurav

---

## 1. What Is CompileTime?

CompileTime is a developer productivity tool that detects idle/waiting moments — build times, Docker pulls, npm installs, CI/CD pipelines, Claude reasoning — and fills them with useful micro-tasks so developers stop doom-scrolling and stay in flow.

The core loop: **you run a build → CompileTime detects it → a 45-second quiz appears in your browser → you answer, earn XP, and the overlay disappears when the build is done.**

It is a long-term product, designed to grow with new modules over time. The MCQ quiz is the first module.

---

## 2. Goals

- Detect when a developer is waiting on a long-running terminal command
- Surface a useful, short micro-task (MCQ quiz to start) without breaking flow
- Track progress through XP, streaks, and achievements over time
- Be non-intrusive — the overlay should never block work or slow the terminal
- Run entirely locally — no accounts, no cloud dependency for core features
- Be cross-platform: macOS, Linux, Windows

## 3. Non-Goals (for now)

- No mobile app
- No team/multiplayer features yet
- No cloud sync yet (local-only in v1)
- No monetisation layer yet
- Not a general browser productivity tool — this is specifically for developers with terminal workflows

---

## 4. System Components

| Component | Description |
|-----------|-------------|
| **Companion App** | Spring Boot service, runs in background, detects terminal commands, drives all logic |
| **Chrome Extension** | MV3 extension, connects to companion via WebSocket, injects quiz overlay |
| **Shell Hooks** | Bash/PowerShell scripts auto-injected into user's shell config by installer |
| **Settings UI** | React app served by Spring Boot at `localhost:9999/settings` |
| **SQLite DB** | Local database for XP, streaks, build history, settings |
| **Question Bank** | JSON files (per category) bundled with the companion app |

---

## 5. MVP Scope (Phase 1)

The MVP proves the full loop works end-to-end:

- [ ] Companion app starts on machine boot (system tray icon visible)
- [ ] Shell hook installer works on macOS/Linux (zsh + bash)
- [ ] `npm run build` is detected and classified correctly
- [ ] 3-second debounce correctly ignores fast commands
- [ ] TRIGGER event fires to Chrome Extension via WebSocket
- [ ] Quiz overlay appears in active Chrome tab
- [ ] MCQ question renders with 4 options and a 45-second timer
- [ ] Answer is evaluated, XP is shown, result is saved to SQLite
- [ ] DONE event fires when build exits, overlay dismisses
- [ ] Minimum 50 questions across 3 categories (javascript, docker, git)
- [ ] Basic XP total visible in extension popup

MVP explicitly excludes: streaks, achievements, Windows support, settings UI, browser-side CI detection, question difficulty progression.

---

## 6. Feature Roadmap

### Phase 1 — Core Pipeline (MVP)
- Spring Boot companion app skeleton
- REST endpoints for shell hooks (`/api/command/start`, `/api/command/done`)
- Command classifier with initial build command list
- 3-second debounce logic
- WebSocket server broadcasting TRIGGER/DONE events
- Chrome Extension with background service worker (WebSocket client)
- Content script quiz overlay (vanilla JS/CSS)
- SQLite schema + XP recording
- Shell hook installer (zsh + bash)
- Question bank: 50 questions (javascript, docker, git)

### Phase 2 — Polish and Windows
- Windows PowerShell shell hook installer
- Windows CMD process monitor (Java ProcessHandle fallback)
- Streak system (daily streaks based on quiz completion)
- Extension popup showing XP total + current streak
- Settings UI (React, served by companion at /settings)
- System tray: right-click menu with status, open settings, quit
- Question bank expansion: 200+ questions across 8 categories
- Difficulty progression (harder questions as XP grows)

### Phase 3 — Browser Detection + More Modules
- Browser-side detection: GitHub Actions, CircleCI, Claude.ai thinking spinner
- To-do task nudge module (surface a pending task during builds)
- Email summary module (Gmail API, show priority unread emails)
- Achievement system (unlockable badges for milestones)
- Question bank: 500+ questions across all major categories

### Phase 4 — Intelligence Layer
- Claude API integration for dynamic question generation
- Questions contextually generated based on the command that triggered the build
  (e.g. running `docker build` → gets Docker-specific questions)
- Spaced repetition: questions the user got wrong resurface more frequently
- Weak area detection: "You struggle with regex — here's a focused session"

### Phase 5 — Cloud and Social (optional, long-term)
- Optional account + cloud sync for XP and progress
- Leaderboards (opt-in)
- Team mode: shared question banks for engineering teams
- Slack/Teams integration for standup prep and message summaries

---

## 7. Folder Structure

```
compiletime/
├── companion/                          # Spring Boot application
│   ├── src/
│   │   └── main/
│   │       ├── java/com/compiletime/
│   │       │   ├── CompileTimeApplication.java
│   │       │   ├── detection/
│   │       │   │   ├── CommandClassifier.java
│   │       │   │   ├── ProcessMonitor.java
│   │       │   │   └── DebounceService.java
│   │       │   ├── websocket/
│   │       │   │   ├── ExtensionSocketHandler.java
│   │       │   │   └── EventPublisher.java
│   │       │   ├── api/
│   │       │   │   ├── CommandController.java
│   │       │   │   ├── QuestionController.java
│   │       │   │   └── StatsController.java
│   │       │   ├── quiz/
│   │       │   │   └── QuestionService.java
│   │       │   ├── domain/
│   │       │   │   ├── BuildSession.java
│   │       │   │   ├── XpRecord.java
│   │       │   │   ├── Streak.java
│   │       │   │   └── Settings.java
│   │       │   └── tray/
│   │       │       └── SystemTrayManager.java
│   │       └── resources/
│   │           ├── application.yml
│   │           ├── questions/
│   │           │   ├── javascript.json
│   │           │   ├── docker.json
│   │           │   ├── git.json
│   │           │   └── ...
│   │           └── static/             # Settings UI built files
│   ├── installer/
│   │   ├── inject-zsh.sh
│   │   ├── inject-bash.sh
│   │   ├── inject-fish.sh
│   │   └── inject-powershell.ps1
│   └── pom.xml
│
├── extension/                          # Chrome Extension MV3
│   ├── background/
│   │   └── service-worker.js
│   ├── content/
│   │   ├── overlay.js
│   │   └── overlay.css
│   ├── popup/
│   │   ├── popup.html
│   │   └── popup.js
│   ├── icons/
│   └── manifest.json
│
├── settings-ui/                        # React settings app
│   ├── src/
│   │   ├── App.jsx
│   │   ├── pages/
│   │   │   ├── Dashboard.jsx           # XP, streaks, stats
│   │   │   ├── Questions.jsx           # Browse/manage question bank
│   │   │   └── Settings.jsx            # Config: commands, timing, etc.
│   │   └── components/
│   └── package.json
│
└── docs/
    ├── ARCHITECTURE.md                 # This file's companion
    └── PROJECT_SPEC.md                 # This file
```

---

## 8. Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| Companion App | Spring Boot | 3.x |
| Language | Java | 21 (LTS) |
| WebSocket | Spring WebSocket | (included in Spring Boot) |
| REST | Spring MVC | (included in Spring Boot) |
| Database | SQLite | via `org.xerial:sqlite-jdbc` |
| ORM | Spring Data JPA | with Hibernate |
| System Tray | Java AWT SystemTray | (built-in JDK) |
| Settings UI | React | 18.x |
| Chrome Extension | Manifest V3 | Vanilla JS |
| Build Tool | Maven | 3.x |
| Distribution | jpackage | (bundled JRE) |
| Future | GraalVM Native Image | (native binary, no JVM) |

---

## 9. Configuration (application.yml)

```yaml
compiletime:
  server:
    port: 9999

  detection:
    debounce-seconds: 3
    build-commands:
      - npm run build
      - npm install
      - npm ci
      - yarn build
      - yarn install
      - docker build
      - docker pull
      - docker-compose up
      - cargo build
      - cargo run
      - gradle build
      - gradlew build
      - mvn install
      - mvn package
      - mvn compile
      - make
      - git clone
      - git pull
      - pip install
      - poetry install
      - go build
      - go mod download

  quiz:
    question-time-seconds: 45
    xp-correct: 10
    xp-incorrect: 2
    xp-timeout: 0

  database:
    path: ${user.home}/.compiletime/data.db
```

---

## 10. Development Phases and Milestones

### Milestone 1 — Skeleton Works End-to-End
Hardcoded trigger (no real detection yet). Pressing a button in the companion app fires a WebSocket event and the quiz overlay appears in Chrome.

**Done when:** Quiz appears and disappears in Chrome based on a manually triggered event.

### Milestone 2 — Real Terminal Detection
Shell hooks are installed. Running `npm run build` in the terminal triggers the quiz automatically.

**Done when:** Full loop works — terminal command → quiz appears → build ends → quiz dismisses.

### Milestone 3 — Data Persistence
XP is saved after each answer. Extension popup shows cumulative XP.

**Done when:** User can see their XP grow across multiple build sessions.

### Milestone 4 — Windows Support
Process Monitor detects builds on Windows CMD. PowerShell hook installer works.

**Done when:** Full loop works on a Windows machine without shell hooks.

### Milestone 5 — Settings and Polish
Settings UI works, question bank has 200+ questions, streaks are tracked.

**Done when:** A user who installs CompileTime can configure it, see their history, and has enough question variety to not repeat for weeks.

---

## 11. Open Questions

These are decisions that have not been made yet and need to be resolved during development:

1. **Distribution mechanism** — How do users install the companion app? Homebrew (macOS), `.exe` installer (Windows), `.deb`/`.rpm` (Linux), or all three?
2. **Auto-start on login** — How does the companion register itself to start on boot? OS-specific approach needed per platform.
3. **Extension question fetching** — Does the extension fetch a question directly from the companion REST API, or does the companion push a question as part of the TRIGGER WebSocket event? (Pushing it in the event is simpler.)
4. **Question bank format** — JSON files bundled in the JAR (current plan) vs. seeded into SQLite on first run. SQLite approach would allow user-added custom questions more easily.
5. **Offline question fallback** — If companion is not running and browser-side detection triggers, does the extension have a local fallback question bank?

---

## 12. What to Tell Claude Code

When starting a new Claude Code session, run this prompt to load full context:

```
Read ARCHITECTURE.md and PROJECT_SPEC.md before we start.
We are building CompileTime — a Chrome Extension + Spring Boot companion app
that detects terminal build commands and shows MCQ quizzes in the browser.

Current milestone: [PASTE CURRENT MILESTONE HERE]
Today's task: [PASTE WHAT YOU WANT TO BUILD TODAY]
```
