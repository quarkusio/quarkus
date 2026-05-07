package io.quarkus.aesh.ssh.deployment;

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
 * Tests that {@code quarkus.aesh.ssh.health.enabled=false} removes the SSH
 * health check from the readiness endpoint.
 */
public class AeshSshHealthDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", "12233")
            .overrideConfigKey("quarkus.aesh.ssh.health.enabled", "false");

    @Test
    public void testSshHealthCheckAbsent() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("checks.name",
                        Matchers.not(Matchers.hasItem("Aesh SSH server health check")));
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

    @CommandDefinition(name = "goodbye", description = "Say goodbye")
    @CliCommand
    public static class GoodbyeCommand implements Command<CommandInvocation> {

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Goodbye!");
            return CommandResult.SUCCESS;
        }
    }
}
