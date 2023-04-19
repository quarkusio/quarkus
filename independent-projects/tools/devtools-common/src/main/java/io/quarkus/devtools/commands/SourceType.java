package io.quarkus.devtools.commands;

import java.util.Collection;
import java.util.Map;

public enum SourceType {
    GROOVY,
    JAVA,
    KOTLIN,
    SCALA;

    private static Map<String, SourceType> EXTENSION_SOURCE_TYPE = Map.of(
            "groovy", GROOVY,
            "quarkus-groovy", GROOVY,
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
