package io.quarkus.aesh.ssh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.annotations.CliCommand;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests that a custom {@code quarkus.aesh.ssh.host} config value is reflected
 * in the SSH health check data.
 */
public class AeshSshCustomHostTest {

    private static final int SSH_PORT = 12231;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    HelloCommand.class,
                    GoodbyeCommand.class))
            .overrideConfigKey("quarkus.aesh.ssh.port", String.valueOf(SSH_PORT))
            .overrideConfigKey("quarkus.aesh.ssh.host", "127.0.0.1");

    @Test
    public void testCustomHostInHealthCheck() {
        RestAssured.when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", CoreMatchers.equalTo("UP"))
                .body("checks.name", Matchers.hasItem("Aesh SSH server health check"))
                .body("checks.find { it.name == 'Aesh SSH server health check' }.data.host",
                        CoreMatchers.equalTo("127.0.0.1"))
                .body("checks.find { it.name == 'Aesh SSH server health check' }.data.port",
                        CoreMatchers.equalTo(SSH_PORT));
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
