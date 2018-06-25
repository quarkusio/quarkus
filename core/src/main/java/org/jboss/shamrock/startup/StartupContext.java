package org.jboss.shamrock.startup;

import java.util.HashMap;
import java.util.Map;

public class StartupContext {

    private final Map<String, Object> values = new HashMap<>();

    public void putValue(String name, Object value) {
        values.put(name, value);
    }

    public Object getValue(String name) {
        return values.get(name);
    }

}
