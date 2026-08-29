package io.quarkus.aesh.deployment;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandNotFoundException;
import org.aesh.command.CommandResult;
import org.aesh.command.Executor;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationConfiguration;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.option.Option;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.settings.SettingsBuilder;
import org.aesh.command.shell.Shell;
import org.aesh.command.validator.CommandValidatorException;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.readline.prompt.Prompt;
import org.aesh.terminal.KeyAction;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.aesh.runtime.CliSettings;
import io.quarkus.test.QuarkusProdModeTest;

/**
 * Tests that a custom {@link CommandInvocationProvider} set via {@link CliSettings}
 * is applied in runtime mode (single top command, {@code AeshRunner} path via
 * {@code DefaultAeshRuntimeRunnerFactory}).
 * <p>
 * This verifies that the fix for aesh#583 (adding {@code commandInvocationProvider()}
 * to {@code AeshRuntimeRunner}) is properly wired through the Quarkus extension.
 */
public class CustomInvocationRuntimeModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    TaggedRuntimeCmd.class,
                    RuntimeTaggedInvocation.class,
                    RuntimeTaggedInvocationProvider.class,
                    RuntimeTaggedCliSettings.class))
            .setApplicationName("custom-invocation-runtime-mode-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("--name=Runtime");

    @Test
    void customInvocationProviderAppliedInRuntimeMode() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("[runtime-tagged] Hello Runtime!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    /**
     * A custom {@link CommandInvocation} subtype that adds a tag prefix.
     */
    public static class RuntimeTaggedInvocation implements CommandInvocation {

        private final CommandInvocation delegate;
        private final String tag;

        RuntimeTaggedInvocation(CommandInvocation delegate, String tag) {
            this.delegate = delegate;
            this.tag = tag;
        }

        public String getTag() {
            return tag;
        }

        @Override
        public Shell getShell() {
            return delegate.getShell();
        }

        @Override
        public void setPrompt(Prompt prompt) {
            delegate.setPrompt(prompt);
        }

        @Override
        public Prompt getPrompt() {
            return delegate.getPrompt();
        }

        @Override
        public String getHelpInfo(String commandName) {
            return delegate.getHelpInfo(commandName);
        }

        @Override
        public String getHelpInfo() {
            return delegate.getHelpInfo();
        }

        @Override
        public void stop() {
            delegate.stop();
        }

        @Override
        public CommandInvocationConfiguration getConfiguration() {
            return delegate.getConfiguration();
        }

        @Override
        public KeyAction input() throws InterruptedException {
            return delegate.input();
        }

        @Override
        public KeyAction input(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.input(timeout, unit);
        }

        @Override
        public String inputLine() throws InterruptedException {
            return delegate.inputLine();
        }

        @Override
        public String inputLine(Prompt prompt) throws InterruptedException {
            return delegate.inputLine(prompt);
        }

        @Override
        public void print(String msg, boolean paging) {
            delegate.print(msg, paging);
        }

        @Override
        public void println(String msg, boolean paging) {
            delegate.println(msg, paging);
        }

        @Override
        public Executor<? extends CommandInvocation> buildExecutor(String line)
                throws CommandNotFoundException, CommandLineParserException,
                OptionValidatorException, CommandValidatorException, IOException {
            return delegate.buildExecutor(line);
        }

        @Override
        public void executeCommand(String input) throws CommandNotFoundException,
                CommandLineParserException, OptionValidatorException,
                CommandValidatorException, CommandException, InterruptedException, IOException {
            delegate.executeCommand(input);
        }
    }

    public static class RuntimeTaggedInvocationProvider
            implements CommandInvocationProvider<RuntimeTaggedInvocation> {

        @Override
        public RuntimeTaggedInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
            return new RuntimeTaggedInvocation(commandInvocation, "runtime-tagged");
        }
    }

    @ApplicationScoped
    public static class RuntimeTaggedCliSettings implements CliSettings {

        @Override
        @SuppressWarnings("unchecked")
        public void customize(SettingsBuilder<?> builder) {
            ((SettingsBuilder) builder).commandInvocationProvider(new RuntimeTaggedInvocationProvider());
        }
    }

    /**
     * Single command — triggers runtime mode auto-detection.
     * Expects {@link RuntimeTaggedInvocation} and uses the tag in output.
     */
    @CommandDefinition(name = "tagged-runtime", description = "Runtime mode with custom invocation")
    public static class TaggedRuntimeCmd implements Command<RuntimeTaggedInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        String name;

        @Override
        public CommandResult execute(RuntimeTaggedInvocation invocation) {
            invocation.println("[" + invocation.getTag() + "] Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }
}
