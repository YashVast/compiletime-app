package com.compiletime.application;

import com.compiletime.infrastructure.ExtensionSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Translates business events into WebSocket JSON messages and broadcasts them
 * to all connected Chrome Extension clients.
 *
 * This sits in the application layer — it knows what events mean (TRIGGER,
 * DONE, ABORT) but delegates the actual sending to ExtensionSocketHandler
 * in the infrastructure layer.
 *
 * JSON formats match the spec in ARCHITECTURE.md §2.5:
 *   TRIGGER  →  {"type":"TRIGGER","cmd":"npm run build","timestamp":1712345678}
 *   DONE     →  {"type":"DONE","exitCode":0,"durationMs":45230}
 *   ABORT    →  {"type":"ABORT"}
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final ExtensionSocketHandler socketHandler;

    public EventPublisher(ExtensionSocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    /**
     * Fired when a build has been running longer than the debounce window.
     * Tells the extension to show the quiz overlay.
     */
    public void publishTrigger(String command) {
        String json = """
                {"type":"TRIGGER","cmd":"%s","timestamp":%d}"""
                .formatted(escape(command), Instant.now().getEpochSecond());

        log.info("[EventPublisher] Broadcasting TRIGGER for command: {}", command);
        socketHandler.broadcast(json);
    }

    /**
     * Fired when the build process exits normally.
     * Tells the extension to dismiss the quiz overlay.
     */
    public void publishDone(int exitCode, long durationMs) {
        String json = """
                {"type":"DONE","exitCode":%d,"durationMs":%d}"""
                .formatted(exitCode, durationMs);

        log.info("[EventPublisher] Broadcasting DONE (exitCode={}, duration={}ms)", exitCode, durationMs);
        socketHandler.broadcast(json);
    }

    /**
     * Fired when the build is interrupted (e.g. Ctrl+C).
     * Tells the extension to dismiss the overlay immediately.
     */
    public void publishAbort() {
        log.info("[EventPublisher] Broadcasting ABORT");
        socketHandler.broadcast("{\"type\":\"ABORT\"}");
    }

    /**
     * Escapes double quotes in a command string so it's safe to embed in JSON.
     * Simple approach sufficient for command strings — not a full JSON encoder.
     */
    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
