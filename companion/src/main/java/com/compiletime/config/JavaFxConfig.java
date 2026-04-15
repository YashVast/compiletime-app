package com.compiletime.config;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * Initializes the JavaFX platform alongside Spring Boot.
 *
 * JavaFX normally expects to own the main thread via Application.launch().
 * Since Spring Boot owns the main thread, we use Platform.startup() instead —
 * this initializes JavaFX without requiring a full Application class.
 *
 * We fire this on ContextRefreshedEvent so all Spring beans are ready
 * before the JavaFX platform starts.
 */
@Component
public class JavaFxConfig implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(JavaFxConfig.class);
    private volatile boolean initialized = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized) return;
        initialized = true;

        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                // Prevent JavaFX from shutting down when the last window closes
                Platform.setImplicitExit(false);
                log.info("[JavaFX] Platform initialized successfully");
                latch.countDown();
            });
            latch.await();
        } catch (IllegalStateException e) {
            // Platform.startup() throws if JavaFX is already initialized — that's fine
            log.info("[JavaFX] Platform was already initialized");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[JavaFX] Interrupted while waiting for platform startup", e);
        }
    }
}
