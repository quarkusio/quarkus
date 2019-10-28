package io.quarkus.runtime;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

public class StartupContext implements Closeable {

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    private final Map<String, Object> values = new HashMap<>();
    private final List<Runnable> shutdownTasks = new ArrayList<>();
    private final List<Runnable> lastShutdownTasks = new ArrayList<>();
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

    public StartupContext() {
        values.put(ShutdownContext.class.getName(), shutdownContext);
    }

    public void putValue(String name, Object value) {
        values.put(name, value);
    }

    public Object getValue(String name) {
        return values.get(name);
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
}
