package com.compiletime.application;

import com.compiletime.infrastructure.ExtensionSocketHandler;
import com.compiletime.infrastructure.OverlayWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Translates business events (TRIGGER, DONE, ABORT) into actions:
 *  1. Shows/hides the JavaFX overlay window (system-wide, appears over any app)
 *  2. Broadcasts JSON over WebSocket to any connected Chrome Extension clients
 *
 * Both channels fire on every event — if the extension is connected it also
 * gets notified, but the primary UI is now the JavaFX native overlay.
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final ExtensionSocketHandler socketHandler;
    private final OverlayWindow overlayWindow;

    public EventPublisher(ExtensionSocketHandler socketHandler, OverlayWindow overlayWindow) {
        this.socketHandler = socketHandler;
        this.overlayWindow = overlayWindow;
    }

    /**
     * Fired when a build has been running longer than the debounce window.
     * Shows the native overlay and notifies the Chrome Extension.
     */
    public void publishTrigger(String command, Long sessionId) {
        log.info("[EventPublisher] TRIGGER — showing overlay for: {}", command);

        // Show native JavaFX overlay (works over any app)
        overlayWindow.show(command, sessionId);

        // Also broadcast to Chrome Extension if connected
        String json = """
                {"type":"TRIGGER","cmd":"%s","timestamp":%d}"""
                .formatted(escape(command), Instant.now().getEpochSecond());
        socketHandler.broadcast(json);
    }

    /**
     * Fired when the build process exits normally.
     * Dismisses the overlay and notifies the Chrome Extension.
     */
    public void publishDone(int exitCode, long durationMs) {
        log.info("[EventPublisher] DONE — dismissing overlay (exitCode={}, duration={}ms)", exitCode, durationMs);

        overlayWindow.hide();

        String json = """
                {"type":"DONE","exitCode":%d,"durationMs":%d}"""
                .formatted(exitCode, durationMs);
        socketHandler.broadcast(json);
    }

    /**
     * Fired when the build is interrupted (e.g. Ctrl+C).
     */
    public void publishAbort() {
        log.info("[EventPublisher] ABORT — dismissing overlay");

        overlayWindow.hide();
        socketHandler.broadcast("{\"type\":\"ABORT\"}");
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
