package io.quarkus.aesh.runtime;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.aesh.AeshConsoleRunner;
import org.aesh.terminal.Connection;
import org.jboss.logging.Logger;

import io.quarkus.aesh.runtime.devmode.AeshHotReplacementInterceptor;
import io.quarkus.aesh.runtime.devmode.DevModeConnection;
import io.quarkus.arc.Arc;

/**
 * CDI bean that sets up aesh command processing on any {@link Connection}.
 * <p>
 * Used by the WebSocket and SSH extensions to provide remote terminal access.
 * Each remote connection gets its own {@link AeshConsoleRunner} with independent
 * readline state, while sharing the same CDI-managed command implementations.
 * <p>
 * This bean depends on {@link CliCommandRegistryFactory}, which is only available
 * in console mode. It should only be used when the application is configured for
 * interactive shell mode.
 */
@ApplicationScoped
public class AeshRemoteConnectionHandler {

    private static final Logger LOG = Logger.getLogger(AeshRemoteConnectionHandler.class);

    @Inject
    CliCommandRegistryFactory registryFactory;

    @Inject
    CliConfig config;

    @Inject
    Instance<CliSettings> customizers;

    @Inject
    @SessionOpened
    Event<AeshSessionEvent> openedEvent;

    @Inject
    @SessionClosed
    Event<AeshSessionEvent> closedEvent;

    private boolean hasOpenObservers;
    private boolean hasCloseObservers;

    @PostConstruct
    void init() {
        hasOpenObservers = !Arc.container()
                .resolveObserverMethods(AeshSessionEvent.class, SessionOpened.Literal.INSTANCE).isEmpty();
        hasCloseObservers = !Arc.container()
                .resolveObserverMethods(AeshSessionEvent.class, SessionClosed.Literal.INSTANCE).isEmpty();
    }

    /**
     * Set up aesh command processing on the given connection.
     * <p>
     * This method may or may not block depending on the connection type.
     * For WebSocket connections, it blocks until close. For SSH connections,
     * it returns immediately (event-driven).
     *
     * @param connection the remote terminal connection (SSH, WebSocket, etc.)
     */
    public void handle(Connection connection) {
        handle(connection, "unknown");
    }

    /**
     * Set up aesh command processing on the given connection.
     * <p>
     * This method may or may not block depending on the connection type.
     * For WebSocket connections, it blocks until close. For SSH connections,
     * it returns immediately (event-driven).
     * <p>
     * The closed event is fired via the close handler when the connection
     * terminates. An {@link AtomicBoolean} ensures the event fires exactly once
     * even if {@code close()} is called multiple times.
     *
     * @param connection the remote terminal connection (SSH, WebSocket, etc.)
     * @param transport the transport type ({@code "ssh"}, {@code "websocket"}, etc.)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void handle(Connection connection, String transport) {
        if (AeshHotReplacementInterceptor.isActive()) {
            connection = new DevModeConnection(connection);
        }

        String sessionId = UUID.randomUUID().toString();
        fireOpenedEvent(sessionId, transport);

        AtomicBoolean closedEventFired = new AtomicBoolean(false);

        Consumer<Void> existingCloseHandler = connection.closeHandler();
        connection.setCloseHandler(v -> {
            if (closedEventFired.compareAndSet(false, true)) {
                fireClosedEvent(sessionId, transport);
            }
            if (existingCloseHandler != null) {
                existingCloseHandler.accept(v);
            }
        });

        try {
            var registryBuilder = registryFactory.create();
            var settingsBuilder = CliSettingsHelper.createBaseSettings(config, customizers);
            settingsBuilder.persistHistory(false);
            var settings = settingsBuilder.build();

            AeshConsoleRunner runner = AeshConsoleRunner.builder()
                    .commandRegistryBuilder(registryBuilder)
                    .settings(settings)
                    .connection(connection)
                    .prompt(config.prompt());

            if (config.addExitCommand()) {
                runner.addExitCommand();
            }

            runner.start();
        } catch (Exception e) {
            LOG.error("Error handling remote connection", e);
            connection.close();
        }
    }

    private void fireOpenedEvent(String sessionId, String transport) {
        if (hasOpenObservers) {
            openedEvent.fireAsync(new AeshSessionEvent(sessionId, transport, Instant.now()));
        }
    }

    private void fireClosedEvent(String sessionId, String transport) {
        if (hasCloseObservers) {
            closedEvent.fireAsync(new AeshSessionEvent(sessionId, transport, Instant.now()));
        }
    }
}
