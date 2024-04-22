package io.quarkus.mongodb.tracing;

import static org.assertj.core.api.Assertions.assertThatNoException;

import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterId;
import com.mongodb.connection.ConnectionDescription;
import com.mongodb.connection.ServerId;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;

import io.opentelemetry.api.OpenTelemetry;

class MongoTracingCommandListenerTest {
    private ConnectionDescription connDescr;
    private MongoTracingCommandListener listener;
    private BsonDocument command;

    @BeforeEach
    void setUp() {
        connDescr = new ConnectionDescription(new ServerId(new ClusterId(), new ServerAddress()));
        listener = new MongoTracingCommandListener(OpenTelemetry.noop());
        command = new BsonDocument();
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

}
