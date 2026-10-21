package io.quarkus.aesh.runtime;

import java.util.List;

/**
 * Build-time metadata about a discovered CLI command, available at runtime through {@link AeshContext}.
 *
 * @param className the fully qualified command class name
 * @param commandName the command name
 * @param description the command description, or {@code null}
 * @param groupCommand whether the command is a group command
 * @param subCommandClassNames the class names of the sub-commands of a group command
 */
public record AeshCommandMetadata(String className, String commandName, String description, boolean groupCommand,
        List<String> subCommandClassNames) {

    public AeshCommandMetadata {
        subCommandClassNames = List.copyOf(subCommandClassNames);
    }
}
