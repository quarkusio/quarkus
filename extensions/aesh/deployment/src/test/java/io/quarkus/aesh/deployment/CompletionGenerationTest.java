package io.quarkus.aesh.deployment;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;
import org.aesh.util.completer.ShellCompletionGenerator;
import org.aesh.util.completer.ShellCompletionGenerator.ShellType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that shell completion script generation works for commands
 * using various aesh features including mixins, enums, and group commands.
 */
public class CompletionGenerationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    CommonOptions.class,
                    DeployCommand.class,
                    AppGroup.class,
                    StatusCommand.class,
                    BuildCommand.class));

    @Test
    public void testBashCompletionGeneration() throws Exception {
        String script = ShellCompletionGenerator.generate(
                ShellType.BASH, DeployCommand.class, "myapp");
        Assertions.assertThat(script).isNotEmpty();
        Assertions.assertThat(script).contains("myapp");
        Assertions.assertThat(script).contains("--env");
        Assertions.assertThat(script).contains("--verbose");
        Assertions.assertThat(script).contains("--format");
    }

    @Test
    public void testZshCompletionGeneration() throws Exception {
        String script = ShellCompletionGenerator.generate(
                ShellType.ZSH, DeployCommand.class, "myapp");
        Assertions.assertThat(script).isNotEmpty();
        Assertions.assertThat(script).contains("myapp");
        Assertions.assertThat(script).contains("--env");
    }

    @Test
    public void testFishCompletionGeneration() throws Exception {
        String script = ShellCompletionGenerator.generate(
                ShellType.FISH, DeployCommand.class, "myapp");
        Assertions.assertThat(script).isNotEmpty();
        Assertions.assertThat(script).contains("myapp");
        Assertions.assertThat(script).contains("env");
    }

    @Test
    public void testGroupCommandCompletion() throws Exception {
        String script = ShellCompletionGenerator.generate(
                ShellType.BASH, AppGroup.class, "myapp");
        Assertions.assertThat(script).isNotEmpty();
        Assertions.assertThat(script).contains("status");
        Assertions.assertThat(script).contains("build");
    }

    @Test
    public void testMixinOptionsInCompletion() throws Exception {
        String script = ShellCompletionGenerator.generate(
                ShellType.BASH, DeployCommand.class, "myapp");
        Assertions.assertThat(script).contains("--format");
        Assertions.assertThat(script).contains("--verbose");
    }

    public static class CommonOptions {

        @Option(name = "format", shortName = 'f', defaultValue = "text")
        public String format;

        @Option(name = "verbose", shortName = 'v', hasValue = false)
        public boolean verbose;
    }

    @CommandDefinition(name = "deploy", description = "Deploy application")
    public static class DeployCommand implements Command<CommandInvocation> {

        @Option(name = "env", description = "Target environment", allowedValues = { "dev", "staging", "prod" })
        String env;

        @Argument(description = "Application name")
        String appName;

        @Mixin
        CommonOptions common;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "app", description = "App management", groupCommands = { StatusCommand.class,
            BuildCommand.class })
    public static class AppGroup implements Command<CommandInvocation> {
        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "status", description = "Show app status")
    public static class StatusCommand implements Command<CommandInvocation> {
        @Mixin
        CommonOptions common;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "build", description = "Build the app")
    public static class BuildCommand implements Command<CommandInvocation> {
        @Option(name = "target", defaultValue = "jar")
        String target;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
