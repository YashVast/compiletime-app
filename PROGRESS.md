# CompileTime — Session Progress

> Last updated: April 17, 2026
> Current milestone: Milestone 2 ✅ Complete → Starting Milestone 3

---

## What Is Built (Current State)

### Spring Boot Companion App (`companion/`)
Full backend running on `localhost:9999`. All layers implemented:

| Layer | Files | Status |
|-------|-------|--------|
| `api/` | CommandController, QuestionController, StatsController | ✅ |
| `application/` | BuildService, CommandClassifier, DebounceService, EventPublisher, QuestionService | ✅ |
| `config/` | DetectionProperties, JavaFxConfig | ✅ |
| `domain/` | BuildSession, Question, XpRecord | ✅ |
| `infrastructure/` | BuildSessionRepository, XpRecordRepository, OverlayWindow | ✅ |

### Key Behaviours Working
- Shell hook pings `/api/command/start?cmd=...`
- `CommandClassifier` checks if command is a known build command
- `DebounceService` waits 3 seconds — ignores fast commands
- `EventPublisher` fires `OverlayWindow.show()` after debounce passes
- `OverlayWindow` renders a transparent, always-on-top JavaFX quiz card (bottom-right corner)
- Overlay has ✕ close button — stays open until user manually dismisses
- Question fetched from bundled JSON files (`resources/questions/`)
- Answer saves `XpRecord` to SQLite at `~/.compiletime/data.db`

### Question Bank
9 questions across 3 categories: `javascript`, `docker`, `git`

### Scripts (`scripts/`)
| File | Purpose | Status |
|------|---------|--------|
| `hooks.ps1` | PSReadLine Enter key hook — fires on every PowerShell command | ✅ |
| `process-monitor.ps1` | Polls every 500ms for build tool processes — works in CMD, Git Bash, any terminal | ✅ |
| `Install-CompileTime.ps1` | One-time installer — patches $PROFILE + copies VBS to Startup folder | ✅ |
| `start-compiletime.vbs` | Silent launcher — starts companion + monitor on Windows boot | ✅ |

### What Was Removed (Intentionally)
- Chrome Extension (replaced by native JavaFX overlay — works over any app)
- WebSocket layer (no longer needed without extension)

---

## Architecture Decisions Made

| Decision | Reason |
|----------|--------|
| Single Maven module, 4-layer packages | Learning-friendly, no multi-module overhead |
| SQLite over Postgres | Local desktop tool — each user has their own DB, zero config |
| JavaFX overlay over Chrome Extension | Works over any app, not just browser |
| `Platform.startup()` not `Application.launch()` | Spring Boot owns the main thread |
| Vanilla questions JSON in classpath | Simple for MVP; SQLite migration planned for Milestone 3 |
| PowerShell PSReadLine hook | Clean pre-command intercept, non-blocking |
| PowerShell process monitor for CMD | CMD has no hook API — polling Win32_Process every 500ms works across all terminals |

---

## How to Run

```bash
# Create data directory (first time only)
mkdir C:\Users\maxis\.compiletime

# Start companion app
cd E:/Compiletime/companion
mvn spring-boot:run

# Start process monitor (new PowerShell window)
& "E:\Compiletime\scripts\process-monitor.ps1"

# Test — run any build command in CMD or PowerShell
timeout /t 10 /nobreak
```

---

## Milestone Status

| Milestone | Description | Status |
|-----------|-------------|--------|
| 1 | Skeleton works end-to-end | ✅ Done |
| 2 | Real terminal detection (shell hooks + auto-start) | ✅ Done |
| 3 | Data persistence (XP saves, stats visible) | 🔲 Next |
| 4 | Windows support (PowerShell hook installer) | ✅ Done (merged into M2) |
| 5 | Settings UI + 200+ questions + streaks | 🔲 Pending |

---

## Milestone 3 — What Needs to Be Built

1. **Stats endpoint** — `/api/stats` returns total XP, streak, questions answered
2. **Stats visible in overlay** — show current XP somewhere in the overlay UI
3. **Streak tracking** — consecutive days with at least one correct answer
4. **XP history** — query past sessions from SQLite

---

## GitHub
Repo: https://github.com/YashVast/compiletime-app
Branch: `main`
Last commit: `feat: process monitor for CMD + fix double-start on VBS`
