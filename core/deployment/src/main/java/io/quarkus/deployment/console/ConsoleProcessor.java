package io.quarkus.deployment.console;

import static io.quarkus.deployment.dev.testing.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.dev.ExceptionNotificationBuildItem;
import io.quarkus.deployment.dev.testing.MessageFormat;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.deployment.dev.testing.TestConsoleHandler;
import io.quarkus.deployment.dev.testing.TestListenerBuildItem;
import io.quarkus.deployment.dev.testing.TestSetupBuildItem;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.deployment.ide.EffectiveIdeBuildItem;
import io.quarkus.deployment.ide.Ide;
import io.quarkus.dev.console.QuarkusConsole;

public class ConsoleProcessor {

    private static final Logger log = Logger.getLogger(ConsoleProcessor.class);

    private static boolean consoleInstalled = false;
    static volatile ConsoleStateManager.ConsoleContext exceptionsConsoleContext;
    static volatile ConsoleStateManager.ConsoleContext devUIConsoleContext;

    /**
     * Installs the interactive console for continuous testing (and other usages)
     * <p>
     * This is only installed for dev and test mode, and runs in the build process rather than
     * a recorder to install this as early as possible
     */
    @BuildStep(onlyIf = IsDevelopment.class)
    @Produce(TestSetupBuildItem.class)
    ConsoleInstalledBuildItem setupConsole(
            final TestConfig config,
            final ConsoleConfig consoleConfig,
            final LaunchModeBuildItem launchModeBuildItem,
            final BuildProducer<TestListenerBuildItem> testListenerBuildItemBuildProducer) {

        if (consoleInstalled) {
            return ConsoleInstalledBuildItem.INSTANCE;
        }
        consoleInstalled = true;
        if (config.console().orElse(consoleConfig.enabled())) {
            ConsoleHelper.installConsole(config, consoleConfig, launchModeBuildItem.isTest());
            ConsoleStateManager.init(QuarkusConsole.INSTANCE, launchModeBuildItem.getDevModeType().get());
            //note that this bit needs to be refactored so it is no longer tied to continuous testing
            if (TestSupport.instance().isEmpty() || config.continuousTesting() == TestConfig.Mode.DISABLED
                    || config.flatClassPath()) {
                return ConsoleInstalledBuildItem.INSTANCE;
            }
            TestConsoleHandler consoleHandler = new TestConsoleHandler(launchModeBuildItem.getDevModeType().get());
            consoleHandler.install();
            testListenerBuildItemBuildProducer.produce(new TestListenerBuildItem(consoleHandler));
        }
        return ConsoleInstalledBuildItem.INSTANCE;
    }

    @Consume(ConsoleInstalledBuildItem.class)
    @Produce(ServiceStartBuildItem.class)
    @BuildStep
    void missingDevUIMessageHandler(Capabilities capabilities) {
        if (capabilities.isPresent(Capability.VERTX_HTTP)) {
            return;
        }

        if (devUIConsoleContext == null) {
            devUIConsoleContext = ConsoleStateManager.INSTANCE.createContext("HTTP");
        }
        devUIConsoleContext.reset(new ConsoleCommand('d', "Dev UI", new ConsoleCommand.HelpState(new Supplier<String>() {
            @Override
            public String get() {
                return MessageFormat.RED;
            }
        }, new Supplier<String>() {
            @Override
            public String get() {
                return "unavailable";
            }
        }), new Runnable() {
            @Override
            public void run() {
                System.out.println("\n" + RED
                        + "For a Quarkus application to have access to the Dev UI, it needs to directly or transitively include the 'quarkus-vertx-http' extension"
                        + RESET + "\n");
            }
        }));
    }

    @Consume(ConsoleInstalledBuildItem.class)
    @BuildStep
    void setupExceptionHandler(BuildProducer<ExceptionNotificationBuildItem> exceptionNotificationBuildItem,
            EffectiveIdeBuildItem ideSupport, LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.isAuxiliaryApplication()) {
            return;
        }
        final AtomicReference<StackTraceElement> lastUserCode = new AtomicReference<>();
        exceptionNotificationBuildItem
                .produce(new ExceptionNotificationBuildItem(new BiConsumer<Throwable, StackTraceElement>() {
                    @Override
                    public void accept(Throwable throwable, StackTraceElement stackTraceElement) {
                        lastUserCode.set(stackTraceElement);
                    }
                }));
        if (exceptionsConsoleContext == null) {
            exceptionsConsoleContext = ConsoleStateManager.INSTANCE.createContext("Exceptions");
        }

        exceptionsConsoleContext.reset(
                new ConsoleCommand('x', "Open last exception (or project) in IDE",
                        new ConsoleCommand.HelpState(new Supplier<String>() {
                            @Override
                            public String get() {
                                return MessageFormat.RED;
                            }
                        }, new Supplier<String>() {
                            @Override
                            public String get() {
                                StackTraceElement throwable = lastUserCode.get();
                                if (throwable == null) {
                                    return "none";
                                }
                                return throwable.getFileName() + ":" + throwable.getLineNumber();
                            }
                        }), new Runnable() {
                            @Override
                            public void run() {
                                StackTraceElement throwable = lastUserCode.get();
                                if (throwable == null) {
                                    launchInIDE(ideSupport.getIde(), List.of("."));
                                    return;
                                }
                                String className = throwable.getClassName();
                                String file = throwable.getFileName();
                                if (className.contains(".")) {
                                    file = className.substring(0, className.lastIndexOf('.') + 1).replace('.',
                                            File.separatorChar)
                                            + file;
                                }
                                Path fileName = Ide.findSourceFile(file);
                                if (fileName == null) {
                                    log.error("Unable to find file: " + file);
                                    return;
                                }
                                List<String> args = ideSupport.getIde().createFileOpeningArgs(
                                        fileName.toAbsolutePath().toString(),
                                        "" + throwable.getLineNumber());
                                launchInIDE(ideSupport.getIde(), args);
                            }
                        }));
    }

    protected void launchInIDE(Ide ide, List<String> args) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String effectiveCommand = ide.getEffectiveCommand();
                    if (effectiveCommand == null || effectiveCommand.isEmpty()) {
                        log.debug("Unable to determine proper launch command for IDE: " + ide);
                        return;
                    }
                    List<String> command = new ArrayList<>();
                    command.add(effectiveCommand);
                    command.addAll(args);
                    log.debugf("Opening IDE with %s", command);
                    new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor(10,
                                    TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("Failed to open IDE", e);
                }
            }
        }, "Launch in IDE Action").start();
    }

    @BuildStep
    @Produce(ServiceStartBuildItem.class)
    void installCliCommands(List<ConsoleCommandBuildItem> commands) {
        ConsoleCliManager
                .setCommands(commands.stream().map(ConsoleCommandBuildItem::getConsoleCommand).collect(Collectors.toList()));
    }

    @BuildStep
    ConsoleCommandBuildItem quitCommand() {
        return new ConsoleCommandBuildItem(new QuitCommand());
    }

    @CommandDefinition(name = "quit", description = "Quit the console", aliases = { "q" })
    public static class QuitCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            QuarkusConsole.INSTANCE.exitCliMode();
            return CommandResult.SUCCESS;
        }
    }

    @BuildStep
    ConsoleCommandBuildItem helpCommand() {
        return new ConsoleCommandBuildItem(new HelpCommand());
    }

    @CommandDefinition(name = "help", description = "Display the command list", aliases = { "h" })
    public static class HelpCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.getShell().writeln("The following commands are available, run them with -h for more info:\n");
            for (var c : ConsoleCliManager.commands) {
                commandInvocation.getShell().writeln(
                        c.getParser().getProcessedCommand().name() + "\t" + c.getParser().getProcessedCommand().description());
            }

            return CommandResult.SUCCESS;
        }
    }
}
