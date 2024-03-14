package io.quarkus.runtime;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

public class StartupContext implements Closeable {

    public static final String RAW_COMMAND_LINE_ARGS = StartupContext.class.getName() + ".raw-command-line-args";

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    private final Map<String, Object> values = new HashMap<>();
    private Object lastValue;
    // this is done to distinguish between the value having never been set and having been set as null
    private boolean lastValueSet = false;
    // the initial capacity was determined experimentally for a standard set of extensions
    private final Deque<Runnable> shutdownTasks = new ConcurrentLinkedDeque<>();
    private final Deque<Runnable> lastShutdownTasks = new ConcurrentLinkedDeque<>();
    private String[] commandLineArgs;
    private String currentBuildStepName;

    public StartupContext() {
        ShutdownContext shutdownContext = new ShutdownContext() {
            @Override
            public void addShutdownTask(Runnable runnable) {
                try {
                    shutdownTasks.addFirst(runnable);
                } catch (Throwable e) {
                    LOG.error("Error adding shutdown event", e);
                }
            }

            @Override
            public void addLastShutdownTask(Runnable runnable) {
                try {
                    lastShutdownTasks.addFirst(runnable);
                } catch (Throwable e) {
                    LOG.error("Error adding last shutdown event", e);
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
        lastValueSet = true;
        this.lastValue = value;
    }

    public Object getValue(String name) {
        return values.get(name);
    }

    public Object getLastValue() {
        return lastValue;
    }

    public boolean isLastValueSet() {
        return lastValueSet;
    }

    @Override
    public void close() {
        runAllInReverseOrder(shutdownTasks);
        shutdownTasks.clear();
        runAllInReverseOrder(lastShutdownTasks);
        lastShutdownTasks.clear();
    }

    private void runAllInReverseOrder(Deque<Runnable> tasks) {
        Deque<Runnable> toClose = new ArrayDeque<>(tasks);
        while (toClose.peek() != null) {
            try {
                var runnable = toClose.poll();
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
