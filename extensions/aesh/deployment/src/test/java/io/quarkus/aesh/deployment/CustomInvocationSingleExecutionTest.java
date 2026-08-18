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
 * is applied when a console mode application executes a single command with
 * command-line arguments (the {@code executeAndExit} path in {@code CliRunner}).
 * <p>
 * This reproduces the scenario from
 * <a href="https://github.com/Hyperfoil/h5m/issues/262">h5m#262</a> where
 * commands expecting a custom {@code CommandInvocation} subtype received
 * {@code DefaultCommandInvocation} and failed with {@code ClassCastException}.
 */
public class CustomInvocationSingleExecutionTest {

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    TaggedGreetCmd.class,
                    DummyCmd.class,
                    TaggedInvocation.class,
                    TaggedInvocationProvider.class,
                    TaggedCliSettings.class))
            .setApplicationName("custom-invocation-single-exec-app")
            .setApplicationVersion("0.1-SNAPSHOT")
            .setExpectExit(true)
            .setRun(true)
            .setCommandLineParameters("tagged-greet", "--name=World");

    @Test
    void customInvocationProviderAppliedInSingleExecution() {
        Assertions.assertThat(config.getStartupConsoleOutput())
                .containsOnlyOnce("[tagged] Hello World!");
        Assertions.assertThat(config.getExitCode()).isZero();
    }

    /**
     * A custom {@link CommandInvocation} subtype that adds a tag prefix
     * to printed output. Similar to h5m's {@code H5mCommandInvocation}.
     */
    public static class TaggedInvocation implements CommandInvocation {

        private final CommandInvocation delegate;
        private final String tag;

        TaggedInvocation(CommandInvocation delegate, String tag) {
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

    /**
     * A custom {@link CommandInvocationProvider} that wraps the default invocation
     * with {@link TaggedInvocation}.
     */
    public static class TaggedInvocationProvider implements CommandInvocationProvider<TaggedInvocation> {

        @Override
        public TaggedInvocation enhanceCommandInvocation(CommandInvocation commandInvocation) {
            return new TaggedInvocation(commandInvocation, "tagged");
        }
    }

    /**
     * A {@link CliSettings} bean that registers the custom invocation provider.
     */
    @ApplicationScoped
    public static class TaggedCliSettings implements CliSettings {

        @Override
        @SuppressWarnings("unchecked")
        public void customize(SettingsBuilder<?> builder) {
            ((SettingsBuilder) builder).commandInvocationProvider(new TaggedInvocationProvider());
        }
    }

    /**
     * A command that expects {@link TaggedInvocation} and uses the tag in its output.
     * Without the fix, this would receive {@code DefaultCommandInvocation} and
     * throw a {@code ClassCastException}.
     */
    @CommandDefinition(name = "tagged-greet", description = "Greet with a tag prefix")
    public static class TaggedGreetCmd implements Command<TaggedInvocation> {

        @Option(shortName = 'n', name = "name", defaultValue = "World")
        String name;

        @Override
        public CommandResult execute(TaggedInvocation invocation) {
            invocation.println("[" + invocation.getTag() + "] Hello " + name + "!");
            return CommandResult.SUCCESS;
        }
    }

    /**
     * A second command to ensure the extension detects console mode (multiple
     * commands) rather than runtime mode (single command).
     */
    @CommandDefinition(name = "dummy", description = "Dummy command")
    public static class DummyCmd implements Command<TaggedInvocation> {

        @Override
        public CommandResult execute(TaggedInvocation invocation) {
            return CommandResult.SUCCESS;
        }
    }
}
