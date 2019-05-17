package io.quarkus.runtime;

import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.runtime.execution.ExecutionContext;
import io.quarkus.runtime.execution.MapValuesExecutionContext;

public class StartupContext implements Closeable {

    private static final Logger LOG = Logger.getLogger(StartupContext.class);

    private final ExecutionContext context;
    private final Map<String, Object> addedItems = new HashMap<>();
    private final Deque<Runnable> shutdownTasks = new ArrayDeque<>();
    private final ShutdownContext shutdownContext = new ShutdownContext() {
        @Override
        public void addShutdownTask(Runnable runnable) {
            shutdownTasks.addLast(runnable);
        }
    };

    public StartupContext() {
        this(ExecutionContext.EMPTY);
    }

    public StartupContext(final ExecutionContext context) {
        this.context = context;
    }

    public void putValue(final String name, final Object value) {
        addedItems.put(name, value);
    }

    public Object getValue(final String name) {
        if (name.equals(ShutdownContext.class.getName())) {
            return shutdownContext;
        } else {
            final Optional<MapValuesExecutionContext> parent = context.optionallyAs(MapValuesExecutionContext.class);
            if (parent.isPresent()) {
                return addedItems.getOrDefault(name, parent.get().getValue(name));
            } else {
                return addedItems.get(name);
            }
        }
    }

    public ExecutionContext addValuesTo(ExecutionContext original) {
        return original.withValues(addedItems);
    }

    public void close() {
        Runnable task;
        for (;;) {
            task = shutdownTasks.pollLast();
            if (task == null) {
                return;
            }
            try {
                task.run();
            } catch (Throwable e) {
                LOG.error("Running a shutdown task failed", e);
            }
        }
    }
}
