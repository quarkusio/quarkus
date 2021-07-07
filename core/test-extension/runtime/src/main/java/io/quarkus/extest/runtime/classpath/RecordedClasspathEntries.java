package io.quarkus.extest.runtime.classpath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecordedClasspathEntries {

    private RecordedClasspathEntries() {
    }

    private static final Map<Phase, Map<String, List<String>>> content;
    static {
        content = new HashMap<>();
        for (Phase phase : Phase.values()) {
            content.put(phase, new HashMap<>());
        }
    }

    public static void put(Phase phase, String resourceName, List<String> classpathEntries) {
        content.get(phase).put(resourceName, classpathEntries);
    }

    public static List<String> get(Phase phase, String resourceName) {
        List<String> entries = content.get(phase).get(resourceName);
        if (entries == null) {
            throw new IllegalStateException("Classpath entries for resource '" + resourceName + "' were not recorded;"
                    + " make sure to set application property 'bt.augment-phase-classpath-resources-to-record' correctly.");
        }
        return entries;
    }

    public enum Phase {
        AUGMENTATION,
        STATIC_INIT,
        RUNTIME_INIT;
    }
}
