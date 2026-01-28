package io.quarkus.aesh.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * Verifies that CDI injection works in deeply nested group sub-commands
 * and across multiple sub-commands with different injected services.
 * <p>
 * Tests two levels of group nesting ({@code root -> user -> create})
 * and multiple CDI beans injected into the leaf sub-command.
 * This guards against regressions where the CDI-aware container builder
 * is not used during command tree construction.
 */
public class NestedGroupCommandCdiInjectionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    RootGroup.class,
                    UserGroup.class,
                    CreateCommand.class,
                    DeleteCommand.class,
                    UserService.class,
                    AuditService.class))
            .setApplicationName("nested-cdi-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("user", "create", "alice");

    @Test
    public void testCdiInjectionInNestedSubCommand() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .contains("Created user: alice")
                .contains("Audit: created alice");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    @CommandDefinition(name = "root", description = "Root", groupCommands = { UserGroup.class })
    public static class RootGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Usage: root <command>");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "user", description = "User management", groupCommands = { CreateCommand.class,
            DeleteCommand.class })
    public static class UserGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println("Usage: user <command>");
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "create", description = "Create a user")
    public static class CreateCommand implements Command<CommandInvocation> {

        @Argument(description = "Username", required = true)
        String username;

        @Inject
        UserService userService;

        @Inject
        AuditService auditService;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(userService.create(username));
            invocation.println(auditService.log("created " + username));
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "delete", description = "Delete a user")
    public static class DeleteCommand implements Command<CommandInvocation> {

        @Argument(description = "Username", required = true)
        String username;

        @Inject
        UserService userService;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            invocation.println(userService.delete(username));
            return CommandResult.SUCCESS;
        }
    }

    @ApplicationScoped
    public static class UserService {
        public String create(String name) {
            return "Created user: " + name;
        }

        public String delete(String name) {
            return "Deleted user: " + name;
        }
    }

    @ApplicationScoped
    public static class AuditService {
        public String log(String action) {
            return "Audit: " + action;
        }
    }
}
