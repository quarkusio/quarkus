package io.quarkus.gradle.application.internal.dev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.dev.BuildOutputChangeKind;
import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputPathChange;

public final class GradleDevOutputChangeMapper {

    private GradleDevOutputChangeMapper() {
    }

    public static BuildOutputChanges toBuildOutputChanges(GradleDevBuildResult result) {
        requireNonNull(result, "result");
        var mainClassChanges = new ArrayList<BuildOutputPathChange>();
        var mainResourceChanges = new ArrayList<BuildOutputPathChange>();
        var testClassChanges = new ArrayList<BuildOutputPathChange>();
        var testResourceChanges = new ArrayList<BuildOutputPathChange>();
        for (GradleDevFileChange change : result.changes()) {
            switch (change.scope()) {
                case MAIN_CLASSES, DEPENDENCY_CLASSES -> addClassChange(mainClassChanges, change);
                case MAIN_RESOURCES, DEPENDENCY_RESOURCES -> addResourceChange(mainResourceChanges, change);
                case TEST_CLASSES -> addClassChange(testClassChanges, change);
                case TEST_RESOURCES -> addResourceChange(testResourceChanges, change);
                case RUNTIME_JARS -> {
                    // Jar-only changes require a dev-mode restart/rebootstrap path, not a hot-reload file event.
                }
            }
        }
        return new BuildOutputChanges(result.sequence(), result.status(), result.failureKind(), mainClassChanges,
                mainResourceChanges, testClassChanges, testResourceChanges, result.failureSummary(), result.diagnosticsPath(),
                result.userInitiated(),
                result.forceRestart());
    }

    private static void addClassChange(List<BuildOutputPathChange> target, GradleDevFileChange change) {
        if (!change.changedPath().getFileName().toString().endsWith(".class")) {
            return;
        }
        target.add(toPathChange(change));
    }

    private static void addResourceChange(List<BuildOutputPathChange> target, GradleDevFileChange change) {
        if (isDirectory(change)) {
            return;
        }
        target.add(toPathChange(change));
    }

    private static boolean isDirectory(GradleDevFileChange change) {
        return change.kind() != BuildOutputChangeKind.DELETED && Files.isDirectory(change.changedPath());
    }

    private static BuildOutputPathChange toPathChange(GradleDevFileChange change) {
        return new BuildOutputPathChange(change.outputRoot(), change.changedPath(), change.kind());
    }
}
