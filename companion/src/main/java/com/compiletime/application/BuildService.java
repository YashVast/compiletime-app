package com.compiletime.application;

import com.compiletime.domain.BuildSession;
import com.compiletime.infrastructure.BuildSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildService.class);

    private final BuildSessionRepository repository;
    private final CommandClassifier classifier;
    private final DebounceService debounceService;

    // Tracks the single active build session (one build at a time for MVP)
    private final AtomicReference<BuildSession> activeSession = new AtomicReference<>();

    public BuildService(BuildSessionRepository repository,
                        CommandClassifier classifier,
                        DebounceService debounceService) {
        this.repository = repository;
        this.classifier = classifier;
        this.debounceService = debounceService;
    }

    /**
     * Called when the shell hook fires on command start.
     * If the command is a known build command, saves a session and starts
     * the debounce timer. Otherwise, ignores it silently.
     *
     * @return the saved BuildSession, or null if command is not a build command
     */
    public BuildSession startBuild(String command) {
        if (!classifier.isBuildCommand(command)) {
            log.debug("[BuildService] Ignored non-build command: {}", command);
            return null;
        }

        BuildSession session = new BuildSession(command, Instant.now());
        session = repository.save(session);
        activeSession.set(session);

        final Long sessionId = session.getId();
        debounceService.schedule(sessionId, () -> onTrigger(sessionId));

        log.info("[BuildService] Build started — session {}: {}", sessionId, command);
        return session;
    }

    /**
     * Called when the shell hook fires on command exit.
     * Cancels the debounce if the build was too fast, or marks the session
     * complete if it was long enough to show a quiz.
     *
     * @param exitCode the shell exit code (0 = success)
     */
    public BuildSession endBuild(int exitCode) {
        BuildSession session = activeSession.getAndSet(null);
        if (session == null) {
            log.debug("[BuildService] No active session to end");
            return null;
        }

        Instant now = Instant.now();
        session.setEndedAt(now);
        session.setDurationMs(now.toEpochMilli() - session.getStartedAt().toEpochMilli());
        session.setExitCode(exitCode);

        // If the debounce is still pending, the build was too fast — cancel quietly
        boolean cancelledBeforeTrigger = debounceService.cancel(session.getId());
        if (cancelledBeforeTrigger) {
            log.info("[BuildService] Build too fast ({}ms) — no quiz shown for session {}",
                    session.getDurationMs(), session.getId());
        }

        return repository.save(session);
    }

    /**
     * Fired by DebounceService after the debounce window passes.
     * This is where the TRIGGER event will be broadcast to the Chrome Extension
     * via WebSocket (to be wired up in the next milestone).
     */
    private void onTrigger(Long sessionId) {
        log.info("[BuildService] TRIGGER — session {} has been running over debounce window. Quiz time!", sessionId);
        // TODO: publish TRIGGER event via EventPublisher (WebSocket) in next milestone
    }
}
