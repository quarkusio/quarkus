package io.quarkus.gradle.application.tasks;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;

import io.quarkus.deployment.dev.BuildOutputChangeKind;
import io.quarkus.deployment.dev.BuildOutputChangeStatus;
import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputChangesDeliveryKind;
import io.quarkus.deployment.dev.BuildOutputFailureKind;
import io.quarkus.gradle.application.internal.dev.GradleDevBuildResult;
import io.quarkus.gradle.application.internal.dev.GradleDevFileChange;
import io.quarkus.gradle.application.internal.dev.GradleDevOutputChangeMapper;
import io.quarkus.gradle.application.internal.dev.GradleDevOutputScope;
import io.quarkus.gradle.application.internal.dev.GradleDevOutputSnapshot;

/**
 * Tracks one Gradle continuous-build iteration without retaining live Gradle model objects.
 * <p>
 * The task resolves and passes immutable output-root paths to the constructor. File collections
 * are used only while querying {@link InputChanges}; they are never retained by this tracker.
 */
final class GradleDevOutputTracker {

    private final List<GradleDevOutputSnapshot.Root> snapshotRoots;
    private final Path snapshotFile;

    GradleDevOutputTracker(List<GradleDevOutputSnapshot.Root> snapshotRoots, Path snapshotFile) {
        this.snapshotRoots = List.copyOf(requireNonNull(snapshotRoots, "snapshotRoots"));
        this.snapshotFile = requireNonNull(snapshotFile, "snapshotFile").normalize();
    }

    ObservedDevChanges observe(InputChanges inputChanges, boolean ready, List<IncrementalInput> inputs)
            throws IOException {
        requireNonNull(inputChanges, "inputChanges");
        requireNonNull(inputs, "inputs");
        if (ready && !inputChanges.isIncremental()) {
            return observeFromSnapshots();
        }

        boolean incremental = inputChanges.isIncremental();
        var changes = new ArrayList<GradleDevFileChange>();
        int runtimeJarChanges = 0;
        for (IncrementalInput input : inputs) {
            if (!incremental && input.scope() == GradleDevOutputScope.RUNTIME_JARS) {
                continue;
            }
            int observed = collectChanges(inputChanges, input.files(), input.scope(), changes);
            if (input.scope() == GradleDevOutputScope.RUNTIME_JARS) {
                runtimeJarChanges += observed;
            }
        }

        if (!incremental) {
            persistCurrentSnapshot();
            return new ObservedDevChanges(false, false, changes, runtimeJarChanges);
        }

        PreviousSnapshot previous = previousSnapshot();
        if (!previous.available()) {
            // Gradle knows its input delta, but the external dev-mode baseline cannot be
            // proven without our receipt. Persist the current state and force rebaseline.
            persistCurrentSnapshot();
            return new ObservedDevChanges(false, ready, changes, runtimeJarChanges);
        }
        previous.snapshot().updatedBy(changes).write(snapshotFile);
        return new ObservedDevChanges(true, false, changes, runtimeJarChanges);
    }

    BuildOutputChanges toBuildOutputChanges(long sequence, ObservedDevChanges observed) {
        if (observed.rebaseline()) {
            return rebaseline(sequence);
        }
        boolean forceRestart = observed.runtimeJarChanges() > 0;
        return GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(sequence,
                BuildOutputChangeStatus.BUILD_SUCCEEDED, observed.changes(),
                runtimeJarFailureSummary(observed.runtimeJarChanges()), null, false,
                forceRestart || !observed.incremental()));
    }

    BuildOutputChanges recoveryRebaseline(long sequence) throws IOException {
        // Recovery must never advertise a new baseline before its durable snapshot exists.
        persistCurrentSnapshot();
        return rebaseline(sequence);
    }

    private ObservedDevChanges observeFromSnapshots() throws IOException {
        GradleDevOutputSnapshot current = currentSnapshot();
        PreviousSnapshot previous = previousSnapshot();
        current.write(snapshotFile);
        if (!previous.available()) {
            return new ObservedDevChanges(false, true, List.of(), 0);
        }
        List<GradleDevFileChange> changes = current.changesSince(previous.snapshot());
        return new ObservedDevChanges(true, false, changes, current.runtimeJarChangesSince(previous.snapshot()));
    }

    private void persistCurrentSnapshot() throws IOException {
        currentSnapshot().write(snapshotFile);
    }

    private PreviousSnapshot previousSnapshot() {
        if (!Files.isRegularFile(snapshotFile)) {
            return PreviousSnapshot.unavailable();
        }
        try {
            GradleDevOutputSnapshot snapshot = GradleDevOutputSnapshot.read(snapshotFile);
            if (snapshot.isEmpty() && Files.size(snapshotFile) > 0) {
                return PreviousSnapshot.unavailable();
            }
            return new PreviousSnapshot(true, snapshot);
        } catch (IOException | RuntimeException e) {
            return PreviousSnapshot.unavailable();
        }
    }

    private GradleDevOutputSnapshot currentSnapshot() throws IOException {
        return GradleDevOutputSnapshot.capture(snapshotRoots);
    }

    private int collectChanges(InputChanges inputChanges, ConfigurableFileCollection files,
            GradleDevOutputScope scope, List<GradleDevFileChange> target) {
        int count = 0;
        for (FileChange change : inputChanges.getFileChanges(files)) {
            Path changedPath = change.getFile().toPath().normalize();
            Path outputRoot = outputRootFor(scope, changedPath);
            target.add(new GradleDevFileChange(scope, outputRoot, changedPath, changeKind(change.getChangeType())));
            count++;
        }
        return count;
    }

    private Path outputRootFor(GradleDevOutputScope scope, Path changedPath) {
        for (GradleDevOutputSnapshot.Root root : snapshotRoots) {
            Path rootPath = root.path().normalize();
            if (root.scope() == scope && changedPath.startsWith(rootPath)) {
                return rootPath;
            }
        }
        throw new IllegalArgumentException("Changed path is not under a declared dev output root: " + changedPath);
    }

    private static BuildOutputChangeKind changeKind(ChangeType changeType) {
        return switch (changeType) {
            case ADDED -> BuildOutputChangeKind.ADDED;
            case MODIFIED -> BuildOutputChangeKind.MODIFIED;
            case REMOVED -> BuildOutputChangeKind.DELETED;
        };
    }

    private static String runtimeJarFailureSummary(int runtimeJarChanges) {
        if (runtimeJarChanges == 0) {
            return null;
        }
        return "Runtime jar dependency changes require restarting quarkusApplicationDev.";
    }

    private static BuildOutputChanges rebaseline(long sequence) {
        return new BuildOutputChanges(sequence, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputFailureKind.NONE,
                List.of(), List.of(), List.of(), List.of(), null, null, false, true,
                BuildOutputChangesDeliveryKind.REBASELINE);
    }

    record IncrementalInput(GradleDevOutputScope scope, ConfigurableFileCollection files) {
        IncrementalInput {
            requireNonNull(scope, "scope");
            requireNonNull(files, "files");
        }
    }

    record ObservedDevChanges(boolean incremental, boolean rebaseline, List<GradleDevFileChange> changes,
            int runtimeJarChanges) {
        ObservedDevChanges {
            changes = List.copyOf(requireNonNull(changes, "changes"));
        }
    }

    private record PreviousSnapshot(boolean available, GradleDevOutputSnapshot snapshot) {
        private static PreviousSnapshot unavailable() {
            return new PreviousSnapshot(false, GradleDevOutputSnapshot.captureEmpty());
        }
    }
}
