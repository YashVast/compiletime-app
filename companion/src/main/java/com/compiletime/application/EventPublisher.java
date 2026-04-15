package com.compiletime.application;

import com.compiletime.infrastructure.OverlayWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Translates business events (TRIGGER, DONE, ABORT) into overlay actions.
 *
 * The primary UI is the JavaFX native overlay — it appears over any application
 * on the screen, not just inside a browser tab.
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final OverlayWindow overlayWindow;

    public EventPublisher(OverlayWindow overlayWindow) {
        this.overlayWindow = overlayWindow;
    }

    /**
     * Fired when a build has been running longer than the debounce window.
     * Shows the native overlay with a random quiz question.
     */
    public void publishTrigger(String command, Long sessionId) {
        log.info("[EventPublisher] TRIGGER — showing overlay for: {}", command);
        overlayWindow.show(command, sessionId);
    }

    /**
     * Fired when the build process exits normally.
     * Dismisses the overlay.
     */
    public void publishDone(int exitCode, long durationMs) {
        log.info("[EventPublisher] DONE — dismissing overlay (exitCode={}, duration={}ms)", exitCode, durationMs);
        overlayWindow.hide();
    }

    /**
     * Fired when the build is interrupted (e.g. Ctrl+C).
     * Dismisses the overlay immediately.
     */
    public void publishAbort() {
        log.info("[EventPublisher] ABORT — dismissing overlay");
        overlayWindow.hide();
    }
}
