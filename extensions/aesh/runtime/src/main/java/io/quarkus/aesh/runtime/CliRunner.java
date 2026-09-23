package io.quarkus.aesh.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.PipelineExecutionListener;
import org.aesh.command.PipelineResult;
import org.aesh.command.StageOutcome;
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
            // Create a PipelineExecutionListener that composes user listener
            // (if any) with the test signal. Passes exit code, error, and
            // stage data through the signal queue as Object[] to cross the
            // classloader boundary safely.
            //
            // Event order from ProcessManager:
            //   1. onStageComplete(stage[0]) .. onStageComplete(stage[N])
            //   2. onPipelineComplete(result)
            //   3. onCommandComplete(commandLine, result, durationMs, error)
            //
            // We collect stage snapshots in onPipelineComplete and include
            // them in the signal sent by onCommandComplete.
            runner.commandExecutionListener(new PipelineExecutionListener() {

                // Collected per-pipeline, consumed by onCommandComplete.
                // AtomicReference gives atomic read-and-clear. Event order
                // (onPipelineComplete before onCommandComplete, same REPL thread)
                // is guaranteed by ProcessManager.firePipelineEvents in aesh 3.18.0.
                private final AtomicReference<List<Object[]>> pendingStages = new AtomicReference<>();

                @Override
                public void onCommandComplete(String commandLine, CommandResult result, long durationMs) {
                    // The 3-arg form carries no error — forward to the 4-arg form
                    // so the test signal is always sent.
                    onCommandComplete(commandLine, result, durationMs, null);
                }

                @Override
                public void onCommandComplete(String commandLine, CommandResult result,
                        long durationMs, Throwable error) {
                    // Take the stage snapshots first so a throwing user listener
                    // cannot suppress the test signal.
                    List<Object[]> stages = pendingStages.getAndSet(null);
                    try {
                        if (userListener != null) {
                            userListener.onCommandComplete(commandLine, result, durationMs, error);
                        }
                    } finally {
                        // Signal: {exitCode, error, stageData}
                        // stageData is null for single commands, List<Object[]> for pipelines
                        signalQueue.offer(new Object[] { result.getExitCode(), error, stages });
                    }
                }

                @Override
                public void onStageComplete(StageOutcome stage) {
                    if (userListener instanceof PipelineExecutionListener pel) {
                        pel.onStageComplete(stage);
                    }
                }

                @Override
                public void onPipelineComplete(PipelineResult result) {
                    // Snapshot before delegating so a throwing user listener
                    // cannot suppress the stage data.
                    pendingStages.set(snapshotStages(result));
                    if (userListener instanceof PipelineExecutionListener pel) {
                        pel.onPipelineComplete(result);
                    }
                }
            });
        } else if (userListener != null) {
            runner.commandExecutionListener(userListener);
        }
    }

    /**
     * Converts pipeline stage outcomes to classloader-safe snapshots.
     * Each entry is {@code Object[] {commandName, stageIndex, stageCount,
     * exitCode, errorMessage, errorClass, durationMs}} — primitives and
     * Strings only, safe to pass through the signal queue.
     * <p>
     * Returns {@code null} when there is no pipeline data (null result,
     * null/empty stage list, or a single stage) so single commands report
     * no stage results. Null stages are skipped defensively.
     */
    static List<Object[]> snapshotStages(PipelineResult result) {
        if (result == null || result.stages() == null || result.stages().size() <= 1) {
            return null;
        }
        List<StageOutcome> stages = result.stages();
        List<Object[]> snapshots = new ArrayList<>(stages.size());
        for (StageOutcome stage : stages) {
            if (stage == null || stage.result() == null) {
                continue;
            }
            Throwable stageError = stage.error();
            snapshots.add(new Object[] {
                    stage.commandName(),
                    stage.stageIndex(),
                    stage.stageCount(),
                    stage.result().getExitCode(),
                    stageError != null ? stageError.getMessage() : null,
                    stageError != null ? stageError.getClass().getName() : null,
                    stage.durationMs()
            });
        }
        return snapshots;
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
