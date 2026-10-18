package io.quarkus.runtime;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Native image system properties are not automatically propagated to runtime anymore.
 */
@Recorder
public class NativeImageRuntimePropertiesRecorder {

    private static final Map<String, String> MAP = new HashMap<>();

    public void setInStaticInit(String name, String value) {
        if (ImageMode.current() == ImageMode.NATIVE_BUILD) {
            MAP.put(name, value);
        }
    }

    @SuppressWarnings("unused")
    public static void doRuntime() {
        for (Map.Entry<String, String> e : MAP.entrySet()) {
            System.setProperty(e.getKey(), e.getValue());
        }
    }

}
