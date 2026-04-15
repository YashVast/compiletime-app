# CompileTime — Architecture Document

> Last updated: April 2026  
> Status: Pre-development / Planning  
> Author: Gaurav

---

## 1. System Overview

CompileTime is a developer productivity tool that detects idle/waiting moments on a developer's machine (builds, installs, docker pulls, CI pipelines) and fills them with useful micro-tasks — primarily MCQ quizzes — to keep developers in flow instead of doom-scrolling.

The system has two main pieces:
- A **Companion App** (Spring Boot, runs as a background service on the user's machine)
- A **Chrome Extension** (MV3, injects the quiz overlay into the active browser tab)

```
┌─────────────────────────────────────────────────────────────────┐
│                        USER'S MACHINE                           │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  COMPANION APP (Spring Boot)             │   │
│  │                                                         │   │
│  │  ┌─────────────────┐    ┌──────────────────────────┐   │   │
│  │  │  Detection Layer │    │   WebSocket Server       │   │   │
│  │  │                 │    │   localhost:9999/ws       │   │   │
│  │  │  ┌───────────┐  │    └──────────┬───────────────┘   │   │
│  │  │  │Shell Hooks│  │               │                    │   │
│  │  │  │zsh/bash/  │  │    ┌──────────▼───────────────┐   │   │
│  │  │  │pwsh/fish  │  │    │       Event Bus           │   │   │
│  │  │  └───────────┘  │    │  TRIGGER / DONE / ABORT  │   │   │
│  │  │  ┌───────────┐  │───▶└──────────────────────────┘   │   │
│  │  │  │  Process  │  │                                    │   │
│  │  │  │  Monitor  │  │    ┌──────────────────────────┐   │   │
│  │  │  │ (Windows  │  │    │   Settings UI             │   │   │
│  │  │  │  CMD fix) │  │    │   (served at /settings)  │   │   │
│  │  │  └───────────┘  │    └──────────────────────────┘   │   │
│  │  │  ┌───────────┐  │                                    │   │
│  │  │  │  Command  │  │    ┌──────────────────────────┐   │   │
│  │  │  │Classifier │  │    │   SQLite Database         │   │   │
│  │  │  └───────────┘  │    │   XP / streaks / history │   │   │
│  │  └─────────────────┘    └──────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                     ▲  WebSocket (ws://)                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  CHROME EXTENSION (MV3)                  │   │
│  │                                                         │   │
│  │  ┌──────────────┐  ┌─────────────┐  ┌───────────────┐  │   │
│  │  │  Background  │  │   Content   │  │  Browser-side │  │   │
│  │  │  Service     │  │   Script    │  │  Detection    │  │   │
│  │  │  Worker      │  │  (Overlay + │  │  (GitHub CI,  │  │   │
│  │  │  (WS Client) │  │   Quiz UI)  │  │   Claude.ai)  │  │   │
│  │  └──────────────┘  └─────────────┘  └───────────────┘  │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Companion App (Spring Boot)

### 2.1 Responsibilities
- Run as a background service/daemon on the user's machine
- Expose REST endpoints that shell hooks ping on every command
- Classify commands to determine if they are "build/wait" type
- Debounce fast commands (ignore anything that finishes in under 3 seconds)
- Broadcast WebSocket events to the Chrome Extension
- Persist XP, streaks, build history, and user settings in SQLite
- Serve the settings UI (React) at `localhost:9999/settings`
- Provide a system tray icon (Java AWT) for quick access and status

### 2.2 Detection Layer

#### Shell Hooks (mac/Linux)
The installer injects hooks into `~/.zshrc`, `~/.bashrc`, or `~/.config/fish/config.fish`. These fire an HTTP ping to the companion app on every terminal command:

```bash
# Injected into ~/.zshrc by CompileTime installer
compiletime_preexec() {
  curl -s "http://localhost:9999/api/command/start?cmd=$(echo "$1" | base64)" &
}
compiletime_precmd() {
  curl -s "http://localhost:9999/api/command/done?exit=$?" &
}
preexec_functions+=(compiletime_preexec)
precmd_functions+=(compiletime_precmd)
```

#### Shell Hooks (Windows PowerShell)
Injected into `$PROFILE`:
```powershell
function Prompt {
    $cmd = (Get-History -Count 1).CommandLine
    Invoke-WebRequest -Uri "http://localhost:9999/api/command/done" -Method POST `
      -Body @{cmd=$cmd; exit=$LASTEXITCODE} -UseBasicParsing | Out-Null
    return "PS $($executionContext.SessionState.Path.CurrentLocation)> "
}
```

#### Process Monitor (Windows CMD fallback)
Windows CMD has no native hook mechanism. The companion app runs a background thread using Java's `ProcessHandle` API to watch for new child processes matching known build tool names (`npm.exe`, `docker.exe`, `cargo.exe`, `gradle.bat`, etc.) and fires the same trigger/done pipeline.

```java
// Polls every 500ms for new matching processes
ProcessHandle.allProcesses()
    .filter(p -> classifier.isBuildProcess(p.info().command()))
    .forEach(detectionService::trackProcess);
```

### 2.3 Command Classifier

Not all terminal commands should trigger the quiz. The classifier evaluates the incoming command string:

```
Command arrives
      ↓
Match against known build command patterns
      ├── npm run build / npm install / npm ci
      ├── yarn build / yarn install
      ├── docker build / docker pull / docker-compose up
      ├── cargo build / cargo run / cargo test
      ├── gradle build / gradlew build
      ├── mvn install / mvn package / mvn compile
      ├── make / cmake / cmake --build
      ├── git clone / git pull (large repos)
      ├── pip install / poetry install / pip sync
      ├── go build / go mod download
      └── kubectl apply / helm install
      ↓
Match found → start 3-second debounce timer
      ↓
Process still running after 3s?
      ├── YES → fire TRIGGER event via WebSocket
      └── NO  → it was instant, ignore silently
      ↓
Watch for process exit signal
      ↓
Fire DONE event → extension dismisses overlay
```

The command list is stored in `application.yml` and is user-configurable — developers can add custom build commands specific to their stack.

### 2.4 REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/command/start` | Shell hook fires when a command begins |
| POST | `/api/command/done` | Shell hook fires when a command exits |
| GET | `/api/status` | Extension polls this to check if companion is alive |
| GET | `/api/questions?category=docker` | Returns a random question from the bank |
| POST | `/api/xp/add` | Records XP earned after a quiz answer |
| GET | `/api/stats` | Returns user's XP, streak, total builds intercepted |
| GET | `/api/settings` | Returns current settings |
| PUT | `/api/settings` | Updates settings |

### 2.5 WebSocket Events

The Chrome Extension maintains a persistent WebSocket connection to `ws://localhost:9999/ws`.

**Companion → Extension:**
```json
// Build detected, show the quiz
{ "type": "TRIGGER", "cmd": "npm run build", "timestamp": 1712345678 }

// Build finished, dismiss overlay
{ "type": "DONE", "exitCode": 0, "durationMs": 45230 }

// Build was cancelled (Ctrl+C)
{ "type": "ABORT" }
```

**Extension → Companion:**
```json
// User answered a question
{ "type": "ANSWER", "questionId": "js-042", "correct": true, "timeMs": 8200 }

// User dismissed the overlay manually
{ "type": "DISMISSED" }
```

### 2.6 Database Schema (SQLite)

```sql
-- Build sessions
CREATE TABLE build_session (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    command TEXT NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME,
    duration_ms INTEGER,
    exit_code INTEGER,
    quiz_completed BOOLEAN DEFAULT FALSE,
    xp_earned INTEGER DEFAULT 0
);

-- XP and progress
CREATE TABLE xp_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER REFERENCES build_session(id),
    question_id TEXT NOT NULL,
    correct BOOLEAN NOT NULL,
    time_ms INTEGER,
    xp_awarded INTEGER NOT NULL,
    recorded_at DATETIME NOT NULL
);

-- Streak tracking
CREATE TABLE streak (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date DATE NOT NULL UNIQUE,
    quizzes_completed INTEGER DEFAULT 0,
    xp_earned INTEGER DEFAULT 0
);

-- User settings
CREATE TABLE settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

---

## 3. Chrome Extension (MV3)

### 3.1 Responsibilities
- Maintain WebSocket connection to companion app
- Detect browser-side waiting moments independently (GitHub Actions, Claude.ai)
- Inject quiz overlay into the active tab when a TRIGGER event arrives
- Dismiss overlay on DONE/ABORT event
- Send answer results back to companion via WebSocket

### 3.2 Component Breakdown

**Background Service Worker (`background/service-worker.js`)**
- Manages WebSocket lifecycle (connect, reconnect on drop)
- Listens for TRIGGER/DONE/ABORT events from companion
- Messages the active tab's content script to show/hide overlay
- Handles browser-side detection (URL pattern matching for CI pages)

**Content Script (`content/overlay.js`)**
- Injected into every tab (runs passively until activated)
- On message from background worker: injects overlay DOM into current page
- Renders quiz UI — question, 4 options, countdown timer, XP feedback
- On answer or timer expiry: reports result back to background worker
- On DONE: removes overlay cleanly

**Browser-side Detection (inside service worker)**
```
URL patterns watched:
  github.com/*/actions/runs/*         → GitHub Actions pipeline running
  app.circleci.com/pipelines/*        → CircleCI pipeline
  jenkins.*/job/*/build               → Jenkins build
  claude.ai/chat/*                    → Claude thinking spinner (DOM watch)
```

### 3.3 Quiz Flow

```
TRIGGER event received by service worker
            ↓
Is user actively typing in the current tab? (check focus state)
            ↓ No
Send SHOW_QUIZ message to active tab's content script
            ↓
Content script injects overlay
  - Fetch question from companion: GET /api/questions
  - Render question + 4 MCQ options
  - Start 45-second countdown
            ↓
User selects answer
  - Highlight correct/incorrect
  - Show XP awarded
  - POST result to companion via background worker
            ↓
DONE event received → overlay fade-out and removal
(or user closes manually → send DISMISSED event)
```

---

## 4. Shell Hook Installer

The companion app ships with an installer script that:
1. Detects the user's default shell
2. Injects the hook into the appropriate config file
3. Backs up the original config before modifying
4. Provides an uninstall command that removes the hooks cleanly

The hooks are non-blocking (fire-and-forget with `&`) so they never slow down the terminal even if the companion app is not running.

---

## 5. Question Bank

Questions are stored as JSON files inside the companion's resources directory, organized by category. Each question follows this schema:

```json
{
  "id": "docker-007",
  "category": "docker",
  "difficulty": "medium",
  "question": "What does the EXPOSE instruction in a Dockerfile actually do?",
  "options": [
    "Opens the port on the host machine automatically",
    "Documents which port the container listens on at runtime",
    "Blocks all other ports from being used",
    "Creates a firewall rule for the container"
  ],
  "correctIndex": 1,
  "explanation": "EXPOSE is documentation only. It does not publish the port. You need -p flag at runtime to actually bind the port.",
  "xp": 10,
  "tags": ["docker", "networking", "containers"]
}
```

Categories to build out over time: `javascript`, `java`, `docker`, `git`, `algorithms`, `linux`, `spring`, `sql`, `regex`, `http`, `system-design`.

---

## 6. Tech Stack Summary

| Layer | Technology | Notes |
|-------|------------|-------|
| Companion App | Spring Boot 3 (Java 21) | Background service, REST + WebSocket |
| WebSocket | Spring WebSocket | Raw WebSocket (not STOMP for simplicity) |
| REST API | Spring MVC | Shell hooks ping these endpoints |
| Database | SQLite + Spring Data JPA | `org.xerial:sqlite-jdbc` driver |
| System Tray | Java AWT `SystemTray` | Basic tray icon, open settings |
| Settings UI | React (served by Spring Boot) | Served at `localhost:9999/settings` |
| Chrome Extension | MV3, Vanilla JS | No framework, lightweight |
| Overlay UI | Vanilla HTML/CSS | Injected into active tab |
| Shell Hooks | Bash / PowerShell scripts | Auto-injected by installer |
| Process Monitor | Java `ProcessHandle` API | Windows CMD fallback detection |
| Distribution | `jpackage` with bundled JRE | Ships as .exe / .dmg / .deb |
| Long-term | GraalVM native image | Native binary, no JVM dependency |

---

## 7. Key Architectural Decisions

**Why Spring Boot over Node/Electron?**
The developer is learning Spring Boot and wants to build real-world experience with it. Spring Boot's WebSocket support, REST capabilities, and Spring Data JPA are all well-suited to this use case. The JVM requirement is acceptable for a developer-targeted tool.

**Why SQLite over PostgreSQL/MySQL?**
This is a local desktop tool. There is no server. SQLite is embedded, ships with the app, requires zero configuration from the user, and is more than sufficient for single-user local data.

**Why Vanilla JS for the extension?**
The Chrome Extension is UI-thin. Adding React or Vue would require a build step and increase bundle size for no meaningful benefit. The overlay is a simple MCQ card — vanilla JS is the right tool.

**Why no MCP integration?**
MCP (Model Context Protocol) is designed for connecting AI assistants to external tools and data sources. CompileTime's core loop (detect build → show quiz → save XP) has no need for an AI model to query its data. If Claude API integration is added later for dynamic question generation, it will be a direct API call from the Spring Boot service — MCP adds no value here.

---

## 8. Security Considerations

- The companion app binds to `localhost` only — never exposed to the network
- WebSocket connection is local-only (`ws://localhost:9999`)
- Shell hooks are fire-and-forget, non-blocking, and non-destructive
- SQLite database is stored in the user's local app data directory
- No user data leaves the machine unless explicitly opted into (future cloud sync feature)
