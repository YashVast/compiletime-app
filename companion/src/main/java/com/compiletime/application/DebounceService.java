package com.compiletime.application;

import com.compiletime.config.DetectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the 3-second debounce window for each build session.
 *
 * When a build command starts, we don't immediately fire TRIGGER — we wait
 * 3 seconds. If the command finishes before that, it was too fast and we
 * ignore it silently. Only if it's still running after 3 seconds do we
 * fire TRIGGER to show the quiz overlay.
 *
 * Each session is tracked by its DB id so multiple concurrent builds
 * don't interfere with each other.
 */
@Service
public class DebounceService {

    private static final Logger log = LoggerFactory.getLogger(DebounceService.class);

    private final DetectionProperties props;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pendingTriggers = new ConcurrentHashMap<>();

    public DebounceService(DetectionProperties props) {
        this.props = props;
    }

    /**
     * Schedules a TRIGGER event for the given session after the debounce window.
     * If the session finishes before the window expires, call cancel(sessionId).
     *
     * @param sessionId the build session id
     * @param onTrigger the action to run when the debounce window passes
     */
    public void schedule(Long sessionId, Runnable onTrigger) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingTriggers.remove(sessionId);
            log.info("[DebounceService] Debounce passed for session {} — firing TRIGGER", sessionId);
            onTrigger.run();
        }, props.getDebounceSeconds(), TimeUnit.SECONDS);

        pendingTriggers.put(sessionId, future);
        log.info("[DebounceService] Started {}s debounce for session {}", props.getDebounceSeconds(), sessionId);
    }

    /**
     * Cancels a pending trigger. Called when the build finishes before the
     * debounce window expires (i.e. the command was too fast to bother the user).
     *
     * @param sessionId the build session id
     * @return true if the trigger was cancelled (command was fast), false if it
     *         already fired (command was slow enough to show quiz)
     */
    public boolean cancel(Long sessionId) {
        ScheduledFuture<?> future = pendingTriggers.remove(sessionId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.info("[DebounceService] Cancelled trigger for session {} (command finished too fast)", sessionId);
            return true;
        }
        return false;
    }

    public boolean hasPendingTrigger(Long sessionId) {
        ScheduledFuture<?> future = pendingTriggers.get(sessionId);
        return future != null && !future.isDone() && !future.isCancelled();
    }
}
