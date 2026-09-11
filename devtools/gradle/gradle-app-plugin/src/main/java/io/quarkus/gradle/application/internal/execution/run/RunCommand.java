package io.quarkus.gradle.application.internal.execution.run;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record RunCommand(
        String name,
        List<String> arguments,
        Optional<Path> workingDirectory,
        Optional<String> startedExpression,
        boolean needsLogfile,
        Optional<Path> logFile) {

    public RunCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Run command requires a name");
        }
        arguments = List.copyOf(arguments);
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Run command requires arguments");
        }
    }
}
