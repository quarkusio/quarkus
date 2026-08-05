package io.quarkus.aesh.deployment;

import java.time.Instant;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshSessionEvent;
import io.quarkus.aesh.runtime.SessionClosed;
import io.quarkus.aesh.runtime.SessionOpened;
import io.quarkus.aesh.runtime.devui.AeshJsonRPCService;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.json.JsonObject;

/**
 * Tests for the Dev UI JSON-RPC service.
 */
public class AeshJsonRPCServiceTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    CalcCommand.class,
                    AeshJsonRPCService.class));

    @Inject
    AeshJsonRPCService jsonRpcService;

    @Test
    public void testGetCommandsReturnsDiscoveredCommands() {
        JsonObject result = jsonRpcService.getCommands();

        Assertions.assertThat(result.getString("mode")).isNotEmpty();
        var commands = result.getJsonArray("commands");
        Assertions.assertThat(commands).isNotNull();
        Assertions.assertThat(commands.size()).isGreaterThanOrEqualTo(2);

        // Verify command metadata fields are present
        var names = new java.util.ArrayList<String>();
        for (int i = 0; i < commands.size(); i++) {
            var cmd = commands.getJsonObject(i);
            names.add(cmd.getString("name"));
            Assertions.assertThat(cmd.getString("className")).isNotEmpty();
            Assertions.assertThat(cmd.containsKey("groupCommand")).isTrue();
        }
        Assertions.assertThat(names).contains("hello", "calc");
    }

    @Test
    public void testGetSessionInfoWithNoTransports() {
        JsonObject result = jsonRpcService.getSessionInfo();

        Assertions.assertThat(result.getJsonArray("transports")).isNotNull();
        Assertions.assertThat(result.getJsonArray("eventLog")).isNotNull();
    }

    @Test
    public void testSessionEventsAreRecorded() {
        // Fire events directly through CDI
        Arc.container().beanManager().getEvent()
                .select(AeshSessionEvent.class, new SessionOpened.Literal())
                .fireAsync(new AeshSessionEvent("test-session-1", "test", Instant.now()))
                .toCompletableFuture().join();

        Arc.container().beanManager().getEvent()
                .select(AeshSessionEvent.class, new SessionClosed.Literal())
                .fireAsync(new AeshSessionEvent("test-session-1", "test", Instant.now()))
                .toCompletableFuture().join();

        // Give async observers time to process
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        JsonObject result = jsonRpcService.getSessionInfo();
        var eventLog = result.getJsonArray("eventLog");
        Assertions.assertThat(eventLog.size()).isGreaterThanOrEqualTo(2);

        // Verify event structure
        var lastEvent = eventLog.getJsonObject(eventLog.size() - 1);
        Assertions.assertThat(lastEvent.getString("eventType")).isIn("opened", "closed");
        Assertions.assertThat(lastEvent.getString("transport")).isEqualTo("test");
        Assertions.assertThat(lastEvent.getString("sessionId")).isEqualTo("test-session-1");
        Assertions.assertThat(lastEvent.getString("timestamp")).isNotEmpty();
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    public static class HelloCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello!");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "calc", description = "Calculate")
    public static class CalcCommand implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
