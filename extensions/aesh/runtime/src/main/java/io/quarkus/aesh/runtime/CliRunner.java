package io.quarkus.aesh.runtime;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;

import org.aesh.AeshConsoleRunner;
import org.aesh.command.AeshCommandRuntimeBuilder;
import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandNotFoundHandler;
import org.aesh.command.CommandResult;
import org.aesh.command.CommandRuntime;
import org.aesh.command.impl.registry.AeshCommandRegistryBuilder;
import org.aesh.command.invocation.CommandInvocation;
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

            var runtimeBuilder = AeshCommandRuntimeBuilder.<CommandInvocation> builder()
                    .commandRegistry(registry);

            if (commandNotFoundHandler.isResolvable()) {
                runtimeBuilder.commandNotFoundHandler(commandNotFoundHandler.get());
            }

            CommandRuntime<CommandInvocation> runtime = runtimeBuilder.build();

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
     * Join command-line arguments into a single command string,
     * quoting arguments that contain spaces.
     */
    private static String joinArgs(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            if (args[i].contains(" ")) {
                sb.append('"').append(args[i]).append('"');
            } else {
                sb.append(args[i]);
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
                    .persistHistory(configuration.persistHistory())
                    .historySize(configuration.historySize())
                    .subCommandModeSettings(subCommandModeSettings);

            if (configuration.historyFile().isPresent()) {
                settingsBuilder.historyFile(new File(configuration.historyFile().get()));
            }

            if (commandNotFoundHandler.isResolvable()) {
                settingsBuilder.commandNotFoundHandler(commandNotFoundHandler.get());
            }

            var settings = settingsBuilder.build();

            AeshConsoleRunner runner = AeshConsoleRunner.builder()
                    .commandRegistryBuilder((AeshCommandRegistryBuilder) registryBuilder)
                    .settings(settings)
                    .prompt(configuration.prompt());

            // Wire user-provided CommandExecutionListener
            if (executionListener.isResolvable()) {
                CommandExecutionListener userListener = executionListener.get();

                // Wire test connection and listener if set by the test framework
                InputStream testInput = AeshTestConnectionHolder.getInput();
                OutputStream testOutput = AeshTestConnectionHolder.getOutput();
                LinkedBlockingQueue<Object> signalQueue = AeshTestConnectionHolder.getSignalQueue();

                if (testInput != null && testOutput != null) {
                    LOG.debug("Test mode: using stream-based connection");
                    runner.connection(new AeshStreamConnection(testInput, testOutput));

                    if (signalQueue != null) {
                        // Compose user listener with test signal
                        runner.commandExecutionListener((commandLine, result, executionTime) -> {
                            userListener.onCommandComplete(commandLine, result, executionTime);
                            signalQueue.offer("done");
                        });
                    } else {
                        runner.commandExecutionListener(userListener);
                    }
                } else {
                    runner.commandExecutionListener(userListener);
                }
            } else {
                // No user listener -- only wire test connection if present
                InputStream testInput = AeshTestConnectionHolder.getInput();
                OutputStream testOutput = AeshTestConnectionHolder.getOutput();
                LinkedBlockingQueue<Object> signalQueue = AeshTestConnectionHolder.getSignalQueue();

                if (testInput != null && testOutput != null) {
                    LOG.debug("Test mode: using stream-based connection");
                    runner.connection(new AeshStreamConnection(testInput, testOutput));

                    if (signalQueue != null) {
                        runner.onCommandComplete(result -> signalQueue.offer("done"));
                    }
                }
            }

            if (configuration.addExitCommand()) {
                runner.addExitCommand();
            }

            runner.start();
            return 0;
        } catch (Exception e) {
            LOG.error("Error starting console", e);
            return 1;
        }
    }
}
