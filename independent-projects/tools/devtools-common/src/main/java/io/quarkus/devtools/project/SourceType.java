package io.quarkus.devtools.project;

import java.util.Collection;
import java.util.Map;

public enum SourceType {
    JAVA,
    KOTLIN,
    SCALA;

    private static Map<String, SourceType> EXTENSION_SOURCE_TYPE = Map.of(
            "kotlin", KOTLIN,
            "quarkus-kotlin", KOTLIN,
            "scala", SCALA,
            "quarkus-scala", SCALA);

    public static SourceType resolve(Collection<String> extensions) {
        for (String extension : extensions) {
            if (EXTENSION_SOURCE_TYPE.containsKey(extension)) {
                return EXTENSION_SOURCE_TYPE.get(extension);
            }
        }
        return JAVA;
    }
}
