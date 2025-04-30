package io.quarkus.runtime;

import java.io.Closeable;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

public class StartupContext implements Closeable {

    public static final String RAW_COMMAND_LINE_ARGS = StartupContext.class.getName() + ".raw-command-line-args";

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    // Holds values for returned proxies
    // These values are usually returned from recorder methods but can be also set explicitly
    // For example, the raw command line args and ShutdownContext are set when the StartupContext is created
    private final Map<String, Object> values = new HashMap<>();

    private final Deque<Runnable> shutdownTasks = new ConcurrentLinkedDeque<>();
    private final Deque<Runnable> lastShutdownTasks = new ConcurrentLinkedDeque<>();
    private String[] commandLineArgs;
    private String currentBuildStepName;

    public StartupContext() {
        ShutdownContext shutdownContext = new ShutdownContext() {
            @Override
            public void addShutdownTask(Runnable runnable) {
                if (runnable != null) {
                    shutdownTasks.addFirst(runnable);
                } else {
                    throw new IllegalArgumentException("Extension passed an invalid shutdown handler");
                }
            }

            @Override
            public void addLastShutdownTask(Runnable runnable) {
                if (runnable != null) {
                    lastShutdownTasks.addFirst(runnable);
                } else {
                    throw new IllegalArgumentException("Extension passed an invalid last shutdown handler");
                }
            }
        };
        values.put(ShutdownContext.class.getName(), shutdownContext);
        values.put(RAW_COMMAND_LINE_ARGS, new Supplier<String[]>() {
            @Override
            public String[] get() {
                if (commandLineArgs == null) {
                    throw new RuntimeException("Command line arguments not available during static init");
                }
                return commandLineArgs;
            }
        });
    }

    public void putValue(String name, Object value) {
        values.put(name, value);
    }

    public Object getValue(String name) {
        return values.get(name);
    }

    @Override
    public void close() {
        runAllAndClear(shutdownTasks);
        runAllAndClear(lastShutdownTasks);
        values.clear();
    }

    private void runAllAndClear(Deque<Runnable> tasks) {
        while (!tasks.isEmpty()) {
            try {
                var runnable = tasks.remove();
                runnable.run();
            } catch (Throwable ex) {
                LOG.error("Running a shutdown task failed", ex);
            }
        }
    }

    @SuppressWarnings("unused")
    public void setCommandLineArguments(String[] commandLineArguments) {
        this.commandLineArgs = commandLineArguments;
    }

    @SuppressWarnings("unused")
    public String getCurrentBuildStepName() {
        return currentBuildStepName;
    }

    @SuppressWarnings("unused")
    public void setCurrentBuildStepName(String currentBuildStepName) {
        this.currentBuildStepName = currentBuildStepName;
    }
}
