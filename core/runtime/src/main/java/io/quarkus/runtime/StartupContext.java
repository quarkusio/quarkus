package io.quarkus.runtime;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

public class StartupContext implements Closeable {

    private static final Object NULL_SENTENTIAL = new Object();

    public static final String RAW_COMMAND_LINE_ARGS = StartupContext.class.getName() + ".raw-command-line-args";

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private Object lastValue;
    // this is done to distinguish between the value never having been set and having been set as null
    private boolean lastValueSet = false;
    private final List<Runnable> shutdownTasks = new CopyOnWriteArrayList<>();
    private final List<Runnable> lastShutdownTasks = new CopyOnWriteArrayList<>();
    private final ShutdownContext shutdownContext = new ShutdownContext() {
        @Override
        public void addShutdownTask(Runnable runnable) {
            shutdownTasks.add(runnable);
        }

        @Override
        public void addLastShutdownTask(Runnable runnable) {
            lastShutdownTasks.add(runnable);
        }
    };
    private String[] commandLineArgs;

    public StartupContext() {
        values.put(ShutdownContext.class.getName(), shutdownContext);
        values.put(StartupContext.class.getName(), this);
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
        if (value == null) {
            value = NULL_SENTENTIAL;
        }
        values.put(name, value);
        lastValueSet = true;
        this.lastValue = value;
    }

    public Object getValue(String name) {
        Object ret = values.get(name);
        if (ret == NULL_SENTENTIAL) {
            return null;
        }
        return ret;
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

    private void runAllInReverseOrder(List<Runnable> tasks) {
        List<Runnable> toClose = new ArrayList<>(tasks);
        Collections.reverse(toClose);
        for (Runnable r : toClose) {
            try {
                r.run();
            } catch (Throwable e) {
                LOG.error("Running a shutdown task failed", e);
            }
        }
    }

    @SuppressWarnings("unused")
    public void setCommandLineArguments(String[] commandLineArguments) {
        this.commandLineArgs = commandLineArguments;
    }
}
