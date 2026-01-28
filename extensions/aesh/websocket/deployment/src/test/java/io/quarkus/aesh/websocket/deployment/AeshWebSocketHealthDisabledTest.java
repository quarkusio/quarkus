package io.quarkus.aesh.websocket.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests that {@code quarkus.aesh.websocket.health.enabled=false} removes the
 * WebSocket health check from the readiness endpoint.
 */
public class AeshWebSocketHealthDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class))
            .overrideConfigKey("quarkus.aesh.websocket.health.enabled", "false");

    @Test
    public void testWebSocketHealthCheckAbsent() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("checks.name",
                        Matchers.not(Matchers.hasItem("Aesh WebSocket terminal health check")));
    }

    @CommandDefinition(name = "hello", description = "Say hello")
    @CliCommand
    public static class HelloCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Hello!");
            return CommandResult.SUCCESS;
        }
    }
}
