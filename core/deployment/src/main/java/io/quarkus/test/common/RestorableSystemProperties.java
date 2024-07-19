package io.quarkus.test.common;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

public class RestorableSystemProperties implements Closeable {
    final Map<String, String> sysPropRestore;

    RestorableSystemProperties(Map<String, String> sysPropRestore) {
        this.sysPropRestore = sysPropRestore;
    }

    public static RestorableSystemProperties setProperties(Map<String, String> props, String... additionalKeysToSave) {
        Map<String, String> sysPropRestore = new HashMap<>();
        for (var i : additionalKeysToSave) {
            sysPropRestore.put(i, System.getProperty(i));
        }
        for (Map.Entry<String, String> i : props.entrySet()) {
            sysPropRestore.put(i.getKey(), System.getProperty(i.getKey()));
        }
        for (Map.Entry<String, String> i : props.entrySet()) {
            System.setProperty(i.getKey(), i.getValue());
        }
        return new RestorableSystemProperties(sysPropRestore);
    }

    @Override
    public void close() {

        for (Map.Entry<String, String> entry : sysPropRestore.entrySet()) {
            String val = entry.getValue();
            if (val == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), val);
            }
        }
    }
}
