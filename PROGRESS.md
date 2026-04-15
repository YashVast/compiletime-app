# CompileTime — Session Progress

> Last updated: April 15, 2026
> Current milestone: Milestone 1 ✅ Complete → Starting Milestone 2

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
- Question fetched from bundled JSON files (`resources/questions/`)
- Answer saves `XpRecord` to SQLite at `~/.compiletime/data.db`
- `/api/command/done` dismisses the overlay

### Question Bank
9 questions across 3 categories: `javascript`, `docker`, `git`

### What Was Removed (Intentionally)
- Chrome Extension (replaced by native JavaFX overlay — works over any app)
- WebSocket layer (no longer needed without extension)

---

## Architecture Decisions Made

| Decision | Reason |
|----------|--------|
| Single Maven module, 4-layer packages | Learning-friendly, no multi-module overhead |
| SQLite over Postgres | Local desktop tool, zero config |
| JavaFX overlay over Chrome Extension | Works over any app, not just browser |
| `Platform.startup()` not `Application.launch()` | Spring Boot owns the main thread |
| Vanilla questions JSON in classpath | Simple for MVP; SQLite migration planned for Phase 2 |

---

## How to Run

```bash
# Create data directory (first time only)
mkdir C:\Users\maxis\.compiletime

# Start companion app
cd E:/Compiletime/companion
mvn spring-boot:run

# Test the loop (in another terminal)
curl -X POST "http://localhost:9999/api/command/start?cmd=npm+run+build"
# wait 3 seconds → overlay appears
curl -X POST "http://localhost:9999/api/command/done?exit=0"
# overlay dismisses
```

---

## Milestone Status

| Milestone | Description | Status |
|-----------|-------------|--------|
| 1 | Skeleton works end-to-end | ✅ Done |
| 2 | Real terminal detection (shell hooks + auto-start) | 🔲 Next |
| 3 | Data persistence (XP saves, stats visible) | 🔲 Pending |
| 4 | Windows support (PowerShell hook installer) | 🔲 Pending |
| 5 | Settings UI + 200+ questions + streaks | 🔲 Pending |

---

## Milestone 2 — What Needs to Be Built

1. **PowerShell hook installer** — script that injects hooks into `$PROFILE`
2. **Hook scripts** — ping `/api/command/start` and `/api/command/done` on every terminal command
3. **Auto-start on Windows boot** — register companion as a startup task (Task Scheduler)

Target: running `npm run build` in any terminal triggers the overlay automatically.

---

## GitHub
Repo: https://github.com/YashVast/compiletime-app
Branch: `main`
Last commit: `refactor: remove Chrome Extension and WebSocket layer`
