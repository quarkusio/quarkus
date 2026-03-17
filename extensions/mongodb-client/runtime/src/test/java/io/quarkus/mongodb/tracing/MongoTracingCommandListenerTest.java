package io.quarkus.mongodb.tracing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bson.BsonDocument;
import org.bson.BsonString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.RequestContext;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterId;
import com.mongodb.connection.ConnectionDescription;
import com.mongodb.connection.ServerId;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.quarkus.mongodb.runtime.MongoRequestContext;
import io.quarkus.mongodb.runtime.MongoTracingRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;

class MongoTracingCommandListenerTest {
    private ConnectionDescription connDescr;
    private MongoTracingCommandListener listener;
    private BsonDocument command;
    private MongoConfig mockConfig;
    private MongoTracingRuntimeConfig mockTracingConfig;
    private OTelRuntimeConfig mockOTelConfig;

    @BeforeEach
    void setUp() {
        connDescr = new ConnectionDescription(new ServerId(new ClusterId(), new ServerAddress()));
        command = new BsonDocument();

        // Create mock configs
        mockConfig = mock(MongoConfig.class);
        mockTracingConfig = mock(MongoTracingRuntimeConfig.class);
        mockOTelConfig = mock(OTelRuntimeConfig.class);

        // Setup default config values
        when(mockConfig.tracing()).thenReturn(mockTracingConfig);
        when(mockTracingConfig.commandDetailLevel())
                .thenReturn(MongoTracingRuntimeConfig.CommandDetailLevel.OFF);
        when(mockOTelConfig.sdkDisabled()).thenReturn(false);

        listener = new MongoTracingCommandListener(OpenTelemetry.noop(), mockConfig, mockOTelConfig);
    }

    @Test
    void commandStarted() {
        var startEvent = new CommandStartedEvent(
                null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                command);
        assertThatNoException().isThrownBy(() -> listener.commandStarted(startEvent));

        CommandSucceededEvent successEvent = new CommandSucceededEvent(null,
                startEvent.getOperationId(),
                startEvent.getRequestId(),
                connDescr,
                startEvent.getDatabaseName(),
                startEvent.getCommandName(),
                startEvent.getCommand(),
                10L);
        assertThatNoException().isThrownBy(() -> listener.commandSucceeded(successEvent));
    }

    @Test
    void mustRemoveOtelContext() {
        RequestContext requestContext = new MongoRequestContext(Context.current());
        assertThat((Context) requestContext.get(MongoRequestContext.OTEL_CONTEXT_KEY)).isNotNull();

        var startEvent = new CommandStartedEvent(
                requestContext,
                1L,
                10,
                connDescr,
                "db",
                "find",
                command);
        listener.commandStarted(startEvent);
        assertThat((Context) requestContext.get(MongoRequestContext.OTEL_CONTEXT_KEY))
                .as("Must remove otel context from request context")
                .isNull();
    }

    @Test
    void commandSucceeded() {
        CommandSucceededEvent cmd = new CommandSucceededEvent(null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                command,
                10L);
        assertThatNoException().isThrownBy(() -> listener.commandSucceeded(cmd));
    }

    @Test
    void commandFailed() {
        var startedEvent = new CommandStartedEvent(
                null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                command);
        assertThatNoException().isThrownBy(() -> listener.commandStarted(startedEvent));

        CommandFailedEvent failedEvent = new CommandFailedEvent(null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                10L,
                new IllegalStateException("command failed"));
        assertThatNoException().isThrownBy(() -> listener.commandFailed(failedEvent));
    }

    @Test
    void commandFailedNoEvent() {
        CommandFailedEvent cmd = new CommandFailedEvent(null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                10L,
                new IllegalStateException("command failed"));
        assertThatNoException().isThrownBy(() -> listener.commandFailed(cmd));
    }

    @Test
    void testCommandDetailLevelOff() {
        when(mockTracingConfig.commandDetailLevel())
                .thenReturn(MongoTracingRuntimeConfig.CommandDetailLevel.OFF);

        listener = new MongoTracingCommandListener(OpenTelemetry.noop(), mockConfig, mockOTelConfig);

        var startEvent = new CommandStartedEvent(
                null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                new BsonDocument("find", new BsonString("users"))
                        .append("filter", new BsonDocument("email", new BsonString("test@example.com"))));

        assertThatNoException().isThrownBy(() -> listener.commandStarted(startEvent));
    }

    @Test
    void testCommandDetailLevelFull() {
        when(mockTracingConfig.commandDetailLevel())
                .thenReturn(MongoTracingRuntimeConfig.CommandDetailLevel.FULL);

        listener = new MongoTracingCommandListener(OpenTelemetry.noop(), mockConfig, mockOTelConfig);

        var startEvent = new CommandStartedEvent(
                null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                new BsonDocument("find", new BsonString("users"))
                        .append("filter", new BsonDocument("email", new BsonString("test@example.com"))));

        assertThatNoException().isThrownBy(() -> listener.commandStarted(startEvent));
    }

    @Test
    void testCommandDetailLevelSanitized() {
        when(mockTracingConfig.commandDetailLevel())
                .thenReturn(MongoTracingRuntimeConfig.CommandDetailLevel.SANITIZED);

        listener = new MongoTracingCommandListener(OpenTelemetry.noop(), mockConfig, mockOTelConfig);

        var startEvent = new CommandStartedEvent(
                null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                new BsonDocument("find", new BsonString("users"))
                        .append("filter", new BsonDocument("email", new BsonString("test@example.com"))));

        assertThatNoException().isThrownBy(() -> listener.commandStarted(startEvent));
    }

    @Test
    void testOtelSdkDisabled() {
        var disabledOTelConfig = mock(OTelRuntimeConfig.class);
        when(disabledOTelConfig.sdkDisabled()).thenReturn(true);
        var disabledListener = new MongoTracingCommandListener(OpenTelemetry.noop(), mockConfig, disabledOTelConfig);

        var startEvent = new CommandStartedEvent(
                null,
                1L,
                10,
                connDescr,
                "db",
                "find",
                new BsonDocument("find", new BsonString("users"))
                        .append("filter", new BsonDocument("email", new BsonString("test@example.com"))));
        assertThatNoException().isThrownBy(() -> disabledListener.commandStarted(startEvent));

        CommandSucceededEvent successEvent = new CommandSucceededEvent(null,
                startEvent.getOperationId(),
                startEvent.getRequestId(),
                connDescr,
                startEvent.getDatabaseName(),
                startEvent.getCommandName(),
                startEvent.getCommand(),
                10L);
        assertThatNoException().isThrownBy(() -> disabledListener.commandSucceeded(successEvent));

        CommandFailedEvent failedEvent = new CommandFailedEvent(null,
                1L,
                11,
                connDescr,
                "db",
                "find",
                10L,
                new IllegalStateException("command failed"));
        assertThatNoException().isThrownBy(() -> disabledListener.commandFailed(failedEvent));
    }

}
