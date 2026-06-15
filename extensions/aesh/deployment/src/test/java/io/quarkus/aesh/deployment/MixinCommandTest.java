package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;
import org.aesh.command.registry.CommandRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that aesh @Mixin support works correctly with the Quarkus extension.
 * A mixin class defines reusable options that can be shared across commands.
 */
public class MixinCommandTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    OutputMixin.class,
                    MixinGreetCommand.class,
                    MixinInfoCommand.class));

    @Inject
    AeshContext aeshContext;

    @Test
    public void testMixinCommandIsDiscovered() {
        Assertions.assertThat(aeshContext.getCommands()).isNotEmpty();
        var greetCmd = aeshContext.getCommands().stream()
                .filter(c -> "greet".equals(c.getCommandName()))
                .findFirst();
        Assertions.assertThat(greetCmd).isPresent();
    }

    @Test
    public void testMixinCommandCanBeRegistered() throws Exception {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder.builder();
        builder.command(MixinGreetCommand.class);
        CommandRegistry<CommandInvocation> registry = builder.create();
        Assertions.assertThat(registry.getAllCommandNames()).contains("greet");
    }

    @Test
    public void testMixinOptionsAreRegistered() throws Exception {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder.builder();
        builder.command(MixinGreetCommand.class);
        CommandRegistry<CommandInvocation> registry = builder.create();
        var parser = registry.getCommand("greet", "greet");
        var options = parser.getParser().getProcessedCommand().getOptions();
        var optionNames = options.stream().map(o -> o.name()).toList();
        Assertions.assertThat(optionNames).contains("name", "format", "verbose");
    }

    @Test
    public void testBothCommandsShareMixin() throws Exception {
        AeshCommandRegistryBuilder<CommandInvocation> builder = AeshCommandRegistryBuilder.builder();
        builder.command(MixinGreetCommand.class);
        builder.command(MixinInfoCommand.class);
        CommandRegistry<CommandInvocation> registry = builder.create();

        var greetOptions = registry.getCommand("greet", "greet")
                .getParser().getProcessedCommand().getOptions().stream().map(o -> o.name()).toList();
        var infoOptions = registry.getCommand("info", "info")
                .getParser().getProcessedCommand().getOptions().stream().map(o -> o.name()).toList();

        Assertions.assertThat(greetOptions).contains("format", "verbose");
        Assertions.assertThat(infoOptions).contains("format", "verbose");
    }

    public static class OutputMixin {

        @Option(name = "format", shortName = 'f', description = "Output format (text or json)", defaultValue = "text")
        public String format;

        @Option(name = "verbose", shortName = 'v', description = "Enable verbose output", hasValue = false)
        public boolean verbose;
    }

    @CommandDefinition(name = "greet", description = "Greet with mixin options")
    public static class MixinGreetCommand implements Command<CommandInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        String name;

        @Mixin
        OutputMixin output;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            String greeting = "Hello " + name + "!";
            if ("json".equals(output.format)) {
                invocation.println("{\"greeting\":\"" + greeting + "\"}");
            } else {
                invocation.println(greeting);
            }
            if (output.verbose) {
                invocation.println("[verbose] format=" + output.format);
            }
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "info", description = "Show info with mixin options")
    public static class MixinInfoCommand implements Command<CommandInvocation> {

        @Mixin
        OutputMixin output;

        @Override
        public CommandResult execute(CommandInvocation invocation) {
            if ("json".equals(output.format)) {
                invocation.println("{\"version\":\"1.0\"}");
            } else {
                invocation.println("App v1.0");
            }
            return CommandResult.SUCCESS;
        }
    }
}
