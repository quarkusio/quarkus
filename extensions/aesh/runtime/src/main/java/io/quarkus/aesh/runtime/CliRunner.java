package io.quarkus.aesh.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.settings.SubCommandModeSettings;
import org.jboss.logging.Logger;

import io.quarkus.runtime.QuarkusApplication;

/**
 * Quarkus application runner that uses AeshConsoleRunner for interactive shell mode.
 * This provides a REPL (Read-Eval-Print Loop) where users can type multiple commands.
 * <p>
 * If command-line arguments are provided, the command is executed once and the
 * application exits (like runtime mode). If no arguments are provided, the
 * interactive REPL starts.
 */
@Dependent
public class CliRunner implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(CliRunner.class);

    private final CliCommandRegistryFactory registryFactory;
    private final CliConfig configuration;
    private final Instance<CliSettings> customizers;
    private final Instance<CommandExecutionListener> executionListener;
    private final Instance<CommandNotFoundHandler> commandNotFoundHandler;

    public CliRunner(CliCommandRegistryFactory registryFactory,
            CliConfig configuration,
            Instance<CliSettings> customizers,
            Instance<CommandExecutionListener> executionListener,
            Instance<CommandNotFoundHandler> commandNotFoundHandler) {
        this.registryFactory = registryFactory;
        this.configuration = configuration;
        this.customizers = customizers;
        this.executionListener = executionListener;
        this.commandNotFoundHandler = commandNotFoundHandler;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int run(String... args) {
        if (args != null && args.length > 0) {
            return executeAndExit(args);
        }
        return startRepl();
    }

    /**
     * Execute a single command from the provided arguments and exit.
     * The arguments are joined into a single command line string
     * (quoting args that contain spaces) and executed against the
     * console mode command registry.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int executeAndExit(String... args) {
        try {
            var registryBuilder = registryFactory.create();
            var registry = ((AeshCommandRegistryBuilder) registryBuilder).create();

            // Build settings with customizers applied so that custom providers
            // (e.g. CommandInvocationProvider) are available for single-command execution.
            // Without this, commands that expect a custom CommandInvocation subtype
            // would receive DefaultCommandInvocation and fail with ClassCastException.
            var settings = CliSettingsHelper.createBaseSettings(configuration, customizers).build();

            var runtimeBuilder = AeshCommandRuntimeBuilder.builder()
                    .commandRegistry(registry)
                    .commandInvocationProvider(settings.commandInvocationProvider())
                    .completerInvocationProvider(settings.completerInvocationProvider())
                    .converterInvocationProvider(settings.converterInvocationProvider())
                    .validatorInvocationProvider(settings.validatorInvocationProvider())
                    .optionActivatorProvider(settings.optionActivatorProvider())
                    .commandActivatorProvider(settings.commandActivatorProvider());

            if (commandNotFoundHandler.isResolvable()) {
                runtimeBuilder.commandNotFoundHandler(commandNotFoundHandler.get());
            }

            CommandRuntime runtime = runtimeBuilder.build();

            String commandLine = joinArgs(args);
            long startTime = System.currentTimeMillis();
            CommandResult result = runtime.executeCommand(commandLine);
            long executionTime = System.currentTimeMillis() - startTime;

            if (result == null) {
                result = CommandResult.SUCCESS;
            }

            if (executionListener.isResolvable()) {
                executionListener.get().onCommandComplete(commandLine, result, executionTime);
            }

            if (result.isSuccess()) {
                return 0;
            }
            return result.getExitCode();
        } catch (Exception e) {
            LOG.error("Error executing command", e);
            return 1;
        }
    }

    /**
     * Wire test connection and command execution listener on the runner.
     * <p>
     * When running under the test framework, {@link AeshTestConnectionHolder}
     * provides piped streams and a signal queue. This method wires them onto
     * the runner and composes the test signal with the optional user-provided
     * listener. Errors are propagated through the signal queue via the 4-arg
     * {@link CommandExecutionListener#onCommandComplete} method.
     *
     * @param runner the console runner to configure
     * @param userListener the user-provided listener, or null if none
     */
    private static void wireTestConnection(AeshConsoleRunner runner, CommandExecutionListener userListener) {
        InputStream testInput = AeshTestConnectionHolder.getInput();
        OutputStream testOutput = AeshTestConnectionHolder.getOutput();
        LinkedBlockingQueue<Object> signalQueue = AeshTestConnectionHolder.getSignalQueue();

        if (testInput == null || testOutput == null) {
            // Not in test mode — just wire the user listener if present
            if (userListener != null) {
                runner.commandExecutionListener(userListener);
            }
            return;
        }

        LOG.debug("Test mode: using stream-based connection");
        runner.connection(new AeshStreamConnection(testInput, testOutput));

        if (signalQueue != null) {
            // Create a listener that composes user listener (if any) with
            // test signal. Passes exit code and error through the signal
            // queue as Object[] to cross the classloader boundary safely.
            runner.commandExecutionListener(new CommandExecutionListener() {
                @Override
                public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                    if (userListener != null) {
                        userListener.onCommandComplete(commandLine, result, durationMs);
                    }
                }

                @Override
                public void onCommandComplete(String commandLine, CommandResult result,
                        long durationMs, Throwable error) {
                    if (userListener != null) {
                        userListener.onCommandComplete(commandLine, result, durationMs, error);
                    }
                    signalQueue.offer(new Object[] { result.getExitCode(), error });
                }
            });
        } else if (userListener != null) {
            runner.commandExecutionListener(userListener);
        }
    }

    /**
     * Join command-line arguments into a properly quoted command string.
     * Arguments containing spaces, quotes, or special characters are quoted.
     */
    private static String joinArgs(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String arg = args[i];
            boolean needsQuoting = arg.isEmpty() || arg.contains(" ") || arg.contains("\n")
                    || arg.contains("\"") || arg.contains("{")
                    || arg.contains("}") || arg.contains("|");
            if (!needsQuoting) {
                sb.append(arg);
            } else if (arg.contains("\"") && !arg.contains("'")) {
                sb.append("'").append(arg).append("'");
            } else {
                sb.append('"').append(arg.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")).append('"');
            }
        }
        return sb.toString();
    }

    /**
     * Start the interactive REPL (Read-Eval-Print Loop).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private int startRepl() {
        try {
            var registryBuilder = registryFactory.create();

            var subCommandModeConfig = configuration.subCommandMode();
            SubCommandModeSettings subCommandModeSettings = SubCommandModeSettings.builder()
                    .enabled(subCommandModeConfig.enabled())
                    .exitCommand(subCommandModeConfig.exitCommand())
                    .alternativeExitCommand(
                            subCommandModeConfig.alternativeExitCommand().isEmpty() ? null
                                    : subCommandModeConfig.alternativeExitCommand())
                    .contextSeparator(subCommandModeConfig.contextSeparator())
                    .showContextOnEntry(subCommandModeConfig.showContextOnEntry())
                    .showArgumentInPrompt(subCommandModeConfig.showArgumentInPrompt())
                    .build();

            var settingsBuilder = CliSettingsHelper.createBaseSettings(configuration, customizers)
                    .subCommandModeSettings(subCommandModeSettings);

            if (commandNotFoundHandler.isResolvable()) {
                settingsBuilder.commandNotFoundHandler(commandNotFoundHandler.get());
            }

            // Wire command output capture for the test framework.
            // When set, ShellOutputTee tees command output (from invocation.println())
            // to this stream, separate from readline prompt/chrome output.
            OutputStream commandOutputCapture = AeshTestConnectionHolder.getCommandOutputCapture();
            if (commandOutputCapture != null) {
                Consumer<String> outputHandler = s -> {
                    try {
                        commandOutputCapture.write(s.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        // ignore
                    }
                };
                settingsBuilder.commandOutputHandler(outputHandler);
            }

            var settings = settingsBuilder.build();

            // Wire input line queue for interactive command testing.
            // The shared queue is populated per-command by AeshLauncherImpl
            // and consumed by invocation.inputLine() in ShellImpl.readLine().
            java.util.Queue<String> inputLineQueue = AeshTestConnectionHolder.getInputLineQueue();
            if (inputLineQueue != null) {
                settings.setInputLineResponses(inputLineQueue);
            }

            AeshConsoleRunner runner = AeshConsoleRunner.builder()
                    .commandRegistryBuilder((AeshCommandRegistryBuilder) registryBuilder)
                    .settings(settings)
                    .prompt(configuration.prompt());

            // Wire test connection and user-provided CommandExecutionListener
            CommandExecutionListener userListener = executionListener.isResolvable()
                    ? executionListener.get()
                    : null;
            wireTestConnection(runner, userListener);

            if (configuration.addExitCommand()) {
                runner.addExitCommand();
            }

            // Signal the test framework when readline is armed and ready
            // for input. The onReady callback fires after readline is armed
            // but before openBlocking(), so execute() can safely send
            // command bytes immediately after launch() returns.
            LinkedBlockingQueue<Object> readySignalQueue = AeshTestConnectionHolder.getSignalQueue();
            if (readySignalQueue != null) {
                runner.onReady(() -> readySignalQueue.offer("ready"));
            }

            runner.start();
            return 0;
        } catch (Exception e) {
            LOG.error("Error starting console", e);
            return 1;
        }
    }
}
