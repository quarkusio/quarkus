package org.jboss.shamrock.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartupContext implements Closeable {

    private final Map<String, Object> values = new HashMap<>();

    private final List<Closeable> resources = new ArrayList<>();

    public void putValue(String name, Object value) {
        values.put(name, value);
    }

    public Object getValue(String name) {
        return values.get(name);
    }

    public void addCloseable(Closeable resource) {
        resources.add(resource);
    }

    @Override
    public void close() {
        List<Closeable> toClose = new ArrayList<>(resources);
        Collections.reverse(toClose);
        for(Closeable r : toClose) {
            try {
                r.close();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }
}
