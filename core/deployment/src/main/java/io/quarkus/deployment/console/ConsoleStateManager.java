package io.quarkus.deployment.console;

import static io.quarkus.deployment.dev.testing.MessageFormat.BLUE;
import static io.quarkus.deployment.dev.testing.MessageFormat.GREEN;
import static io.quarkus.deployment.dev.testing.MessageFormat.NO_UNDERLINE;
import static io.quarkus.deployment.dev.testing.MessageFormat.RED;
import static io.quarkus.deployment.dev.testing.MessageFormat.RESET;
import static io.quarkus.deployment.dev.testing.MessageFormat.UNDERLINE;
import static org.jboss.logmanager.Level.DEBUG;
import static org.jboss.logmanager.Level.ERROR;
import static org.jboss.logmanager.Level.FATAL;
import static org.jboss.logmanager.Level.INFO;
import static org.jboss.logmanager.Level.TRACE;
import static org.jboss.logmanager.Level.WARN;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jboss.logmanager.LogManager;

import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.spi.DevModeType;

public class ConsoleStateManager {

    public static final ConsoleStateManager INSTANCE = new ConsoleStateManager();

    private final Map<Character, Holder> commands = new ConcurrentHashMap<>();

    private org.jboss.logmanager.Level currentLevel;
    private List<Runnable> restoreLogLevelsTasks;
    private volatile String oldPrompt;

    private Consumer<int[]> consumer = new Consumer<int[]>() {

        StringBuilder readLineBuilder;
        Consumer<String> readLineConsumer;

        @Override
        public void accept(int[] ints) {
            for (int i : ints) {
                if (readLineBuilder != null) {
                    if (i == '\n') {
                        readLineConsumer.accept(readLineBuilder.toString());
                        readLineBuilder = null;
                        readLineConsumer = null;
                    } else {
                        readLineBuilder.append((char) i);
                    }
                } else {
                    if (i == '\n') {
                        System.out.println("");
                    }
                    Holder command = commands.get((char) i);
                    if (command != null) {
                        if (command.consoleCommand.getReadLineHandler() != null) {
                            QuarkusConsole.INSTANCE.doReadLine();
                            readLineBuilder = new StringBuilder();
                            readLineConsumer = command.consoleCommand.getReadLineHandler();
                        } else {
                            command.consoleCommand.getRunnable().run();
                        }
                    }
                }
            }
        }
    };

    static volatile boolean initialized = false;

    public static void init(QuarkusConsole console, DevModeType devModeType) {
        if (initialized) {
            return;
        }
        initialized = true;
        if (console.isInputSupported()) {
            console.setInputHandler(INSTANCE.consumer);
            INSTANCE.installBuiltins(devModeType);
        }
    }

    void installBuiltins(DevModeType devModeType) {
        List<ConsoleCommand> commands = new ArrayList<>();
        if (devModeType != DevModeType.TEST_ONLY) {
            commands.add(new ConsoleCommand('s', "Force restart", null, () -> {
                forceRestart();
            }));

            commands.add(new ConsoleCommand('i', "Toggle instrumentation based reload",
                    new ConsoleCommand.HelpState(() -> RuntimeUpdatesProcessor.INSTANCE.instrumentationEnabled()), () -> {
                        //TODO: this is a bit yuck, needed for the websocket listener which is tied to the test listeners
                        if (TestSupport.instance().isPresent()) {
                            TestSupport.instance().get().toggleInstrumentation();
                        } else {
                            RuntimeUpdatesProcessor.INSTANCE.toggleInstrumentation();
                        }
                    }));
            commands.add(new ConsoleCommand('l', "Toggle live reload",
                    new ConsoleCommand.HelpState(() -> RuntimeUpdatesProcessor.INSTANCE.isLiveReloadEnabled()),
                    () -> RuntimeUpdatesProcessor.INSTANCE.toggleLiveReloadEnabled()));
        }

        ConsoleContext context = createContext("System");

        commands.add(new ConsoleCommand('j', "Toggle log levels",
                new ConsoleCommand.HelpState(() -> currentLevel == null ? BLUE : RED,
                        () -> (currentLevel == null
                                ? toLevel(((LogManager) LogManager.getLogManager()).getLogger("").getLevel()).toString()
                                : currentLevel.toString())),
                ConsoleStateManager.this::toggleLogLevel));
        commands.add(new ConsoleCommand((char) 13, null, null, 10001, null, this::printBlankLine));
        commands.add(new ConsoleCommand('h', "Shows this help", "for more options", 10000, null, this::printHelp));
        commands.add(new ConsoleCommand('q', "Quits the application", null, this::exitQuarkus));
        context.reset(commands.toArray(new ConsoleCommand[0]));
    }

    private void forceRestart() {
        RuntimeUpdatesProcessor.INSTANCE.doScan(true, true);
    }

    private void exitQuarkus() {
        //we don't call Quarkus.exit() here as that would just result
        //in a 'press any key to restart' prompt
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.exit(0);
            }
        }, "Quarkus exit thread").run();
    }

    private void toggleLogLevel() {
        if (currentLevel == null) {
            restoreLogLevelsTasks = new ArrayList<>();
            Iterator<String> names = LogManager.getLogManager().getLoggerNames().asIterator();
            while (names.hasNext()) {
                String name = names.next();
                java.util.logging.Logger logger = LogManager.getLogManager().getLogger(name);
                Level level = logger.getLevel();
                restoreLogLevelsTasks.add(new Runnable() {
                    @Override
                    public void run() {
                        logger.setLevel(level);
                    }
                });
                logger.setLevel(DEBUG);
            }
            currentLevel = DEBUG;
            System.out.println("Set log level to DEBUG");
        } else if (currentLevel == DEBUG) {
            Iterator<String> names = LogManager.getLogManager().getLoggerNames().asIterator();
            while (names.hasNext()) {
                String name = names.next();
                java.util.logging.Logger logger = LogManager.getLogManager().getLogger(name);
                logger.setLevel(TRACE);
            }
            System.out.println("Set log level to TRACE");
            currentLevel = TRACE;
        } else {
            for (var i : restoreLogLevelsTasks) {
                i.run();
            }
            restoreLogLevelsTasks = null;
            currentLevel = null;
            System.out.println("Restored log levels to configured values");
        }
    }

    private void printBlankLine() {
        System.out.println("");
    }

    private void printHelp() {
        System.out.println("\nThe following commands are currently available:");
        Set<ConsoleContext> contexts = new HashSet<>();
        for (Holder i : commands.values()) {
            contexts.add(i.context);
        }

        for (ConsoleContext ctx : contexts.stream().sorted(Comparator.comparing(ConsoleContext::getName))
                .collect(Collectors.toList())) {
            System.out.println("\n" + RED + "==" + RESET + " " + UNDERLINE + ctx.name + NO_UNDERLINE + "\n");
            for (var i : ctx.internal) {
                if (i.getDescription() != null) {
                    if (i.getHelpState() == null) {
                        System.out.println(helpOption(i.getKey(), i.getDescription()));
                    } else if (i.getHelpState().toggleState != null) {
                        System.out.println(helpOption(i.getKey(), i.getDescription(), i.getHelpState().toggleState.get()));
                    } else {
                        System.out.println(helpOption(i.getKey(), i.getDescription(), i.getHelpState().stateSupplier.get(),
                                i.getHelpState().colorSupplier.get()));
                    }
                }
            }
        }

    }

    static class Holder {
        final ConsoleCommand consoleCommand;
        final ConsoleContext context;

        Holder(ConsoleCommand consoleCommand, ConsoleContext context) {
            this.consoleCommand = consoleCommand;
            this.context = context;
        }
    }

    public ConsoleContext createContext(String name) {
        return new ConsoleContext(name);
    }

    void redraw() {

        List<ConsoleCommand> sorted = commands.values().stream().map(s -> s.consoleCommand)
                .filter(s -> s.getPromptString() != null).sorted(Comparator.comparingInt(ConsoleCommand::getPromptPriority))
                .collect(Collectors.toList());
        if (sorted.isEmpty()) {
            QuarkusConsole.INSTANCE.setPromptMessage(null);
        } else {
            StringBuilder sb = new StringBuilder("Press");
            boolean first = true;
            for (ConsoleCommand i : sorted) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(" [")
                        .append(BLUE)
                        .append(i.getKey() == ' ' ? "space" : (i.getKey() + ""))
                        .append(RESET)
                        .append("] ")
                        .append(i.getPromptString());
            }
            sb.append(">");
            String newPrompt = sb.toString();
            if (!Objects.equals(newPrompt, oldPrompt)) {
                oldPrompt = newPrompt;
                QuarkusConsole.INSTANCE.setPromptMessage(sb.toString());
            }
        }
    }

    public class ConsoleContext {

        private final String name;
        private final List<ConsoleCommand> internal = new ArrayList<>();

        public ConsoleContext(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void addCommand(ConsoleCommand consoleCommand) {
            addCommandInternal(consoleCommand);
            redraw();
        }

        public void addCommandInternal(ConsoleCommand consoleCommand) {
            synchronized (commands) {
                if (commands.containsKey(consoleCommand.getKey())) {
                    throw new RuntimeException("Key " + consoleCommand.getKey() + " already registered");
                }
                commands.put(consoleCommand.getKey(), new Holder(consoleCommand, this));
                internal.add(consoleCommand);
            }
        }

        public void reset(ConsoleCommand... command) {
            synchronized (commands) {
                internal.clear();
                var it = commands.entrySet().iterator();
                while (it.hasNext()) {
                    var holder = it.next();
                    if (holder.getValue().context == this) {
                        it.remove();
                    }
                }
            }
            for (var i : command) {
                addCommandInternal(i);
            }
            redraw();
        }
    }

    public static String helpOption(char key, String description) {
        return "[" + BLUE + key + RESET + "] - " + description;
    }

    public static String helpOption(char key, String description, boolean enabled) {
        return helpOption(key, description) + toggleStatus(enabled);
    }

    public static String helpOption(char key, String description, String status, String color) {
        return helpOption(key, description) + " (" + color + status + RESET + ")";
    }

    public static String toggleStatus(boolean enabled) {
        return " (" + (enabled ? GREEN + "enabled" + RESET + "" : RED + "disabled") + RESET + ")";
    }

    private org.jboss.logmanager.Level toLevel(Level level) {
        if (level.intValue() >= FATAL.intValue()) {
            return FATAL;
        } else if (level.intValue() >= ERROR.intValue()) {
            return ERROR;
        } else if (level.intValue() >= WARN.intValue()) {
            return WARN;
        } else if (level.intValue() >= INFO.intValue()) {
            return INFO;
        } else if (level.intValue() >= DEBUG.intValue()) {
            return DEBUG;
        }
        return TRACE;
    }
}
