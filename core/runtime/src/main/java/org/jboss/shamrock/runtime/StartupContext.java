package org.jboss.shamrock.runtime;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartupContext implements Closeable {

    private final Map<String, Object> values = new HashMap<>();
    private final List<Runnable> shutdownTasks = new ArrayList<>();
    private final ShutdownContext shutdownContext = new ShutdownContext() {
        @Override
        public void addShutdownTask(Runnable runnable) {
            shutdownTasks.add(runnable);
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
        List<Runnable> toClose = new ArrayList<>(shutdownTasks);
        Collections.reverse(toClose);
        for (Runnable r : toClose) {
            try {
                r.run();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        shutdownTasks.clear();
    }
}
