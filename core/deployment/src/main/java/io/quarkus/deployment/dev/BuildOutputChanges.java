package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

public record BuildOutputChanges(
        long sequence,
        BuildOutputChangeStatus status,
        List<BuildOutputPathChange> mainClassChanges,
        List<BuildOutputPathChange> mainResourceChanges,
        List<BuildOutputPathChange> testClassChanges,
        List<BuildOutputPathChange> testResourceChanges,
        String failureSummary,
        Path diagnosticsPath,
        boolean userInitiated,
        boolean forceRestart) {

    public BuildOutputChanges {
        requireNonNull(status, "status");
        mainClassChanges = copyOrEmpty(mainClassChanges);
        mainResourceChanges = copyOrEmpty(mainResourceChanges);
        testClassChanges = copyOrEmpty(testClassChanges);
        testResourceChanges = copyOrEmpty(testResourceChanges);
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
