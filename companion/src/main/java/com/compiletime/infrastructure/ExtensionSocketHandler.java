package com.compiletime.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all WebSocket connections from Chrome Extension clients.
 *
 * Responsibilities:
 *  - Track every connected extension session
 *  - Remove sessions that disconnect
 *  - Broadcast JSON event strings to all connected sessions
 *
 * This is infrastructure — it knows nothing about business logic.
 * It just sends whatever string it's given to whoever is connected.
 */
@Component
public class ExtensionSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ExtensionSocketHandler.class);

    // CopyOnWriteArrayList is thread-safe: reads are cheap, writes (connect/disconnect)
    // are rare so the copy-on-write cost is acceptable here.
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("[WebSocket] Extension connected — session {}, total connected: {}",
                session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("[WebSocket] Extension disconnected — session {}, total connected: {}",
                session.getId(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WebSocket] Transport error on session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * Broadcasts a raw JSON string to all currently connected extension clients.
     * Sessions that fail to receive are removed from the list.
     *
     * @param json the JSON event string to send (e.g. {"type":"TRIGGER",...})
     */
    public void broadcast(String json) {
        if (sessions.isEmpty()) {
            log.debug("[WebSocket] No extension connected — skipping broadcast: {}", json);
            return;
        }

        TextMessage message = new TextMessage(json);
        sessions.removeIf(session -> {
            if (!session.isOpen()) {
                return true; // remove stale session
            }
            try {
                session.sendMessage(message);
                log.debug("[WebSocket] Sent to session {}: {}", session.getId(), json);
                return false; // keep session
            } catch (IOException e) {
                log.error("[WebSocket] Failed to send to session {} — removing: {}", session.getId(), e.getMessage());
                return true; // remove broken session
            }
        });
    }

    public int getConnectedCount() {
        return sessions.size();
    }
}
