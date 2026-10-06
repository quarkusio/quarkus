package io.quarkus.gradle.application.internal.execution;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RunRequest(
        BuildRequest build,
        Path packageResultFile,
        Optional<String> runTarget,
        List<String> jvmArguments,
        List<String> applicationArguments,
        Map<String, String> environment,
        Path workingDirectory) {

    public RunRequest {
        if (build == null) {
            throw new IllegalArgumentException("Quarkus application run request requires a build request");
        }
        if (packageResultFile == null) {
            throw new IllegalArgumentException("Quarkus application run request requires a package result file");
        }
        runTarget = runTarget == null ? Optional.empty() : runTarget;
        jvmArguments = List.copyOf(jvmArguments);
        applicationArguments = List.copyOf(applicationArguments);
        environment = Map.copyOf(environment);
        if (workingDirectory == null) {
            throw new IllegalArgumentException("Quarkus application run request requires a working directory");
        }
    }
}
