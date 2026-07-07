package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mockito;

import io.quarkus.deployment.dev.BuildOutputChangeKind;
import io.quarkus.deployment.dev.BuildOutputChangeStatus;
import io.quarkus.deployment.dev.BuildOutputChanges;
import io.quarkus.deployment.dev.BuildOutputChangesDeliveryKind;
import io.quarkus.deployment.dev.BuildOutputFailureKind;
import io.quarkus.gradle.application.internal.dev.GradleDevOutputScope;
import io.quarkus.gradle.application.internal.dev.GradleDevOutputSnapshot;
import io.quarkus.gradle.application.internal.dev.QuarkusApplicationDevDeploymentHandle;

class GradleDevOutputTrackerTest {

    @TempDir
    Path testDirectory;

    private Project project;
    private ConfigurableFileCollection classes;
    private Path classesDirectory;
    private Path snapshotFile;

    @BeforeEach
    void setUp() throws Exception {
        project = ProjectBuilder.builder().withProjectDir(testDirectory.toFile()).build();
        classesDirectory = Files.createDirectories(testDirectory.resolve("classes"));
        classes = project.files(classesDirectory.toFile());
        snapshotFile = testDirectory.resolve("snapshot/dev-output.tsv");
    }

    @Test
    void nonIncrementalReadyInputDiffsAndPersistsWholeTreeSnapshots() throws Exception {
        Path applicationClass = Files.writeString(classesDirectory.resolve("Application.class"), "before");
        snapshot().write(snapshotFile);
        Files.writeString(applicationClass, "after");
        GradleDevOutputTracker tracker = tracker();

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(
                inputChanges(false, List.of()), true, inputs());

        assertThat(observed.incremental()).isTrue();
        assertThat(observed.changes()).singleElement()
                .satisfies(change -> {
                    assertThat(change.kind()).isEqualTo(BuildOutputChangeKind.MODIFIED);
                    assertThat(change.changedPath()).isEqualTo(applicationClass);
                });
        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
    }

    @Test
    void missingSnapshotForIncrementalInputPersistsCurrentStateAndForcesRebaseline() throws Exception {
        Path applicationClass = Files.writeString(classesDirectory.resolve("Application.class"), "class");
        GradleDevOutputTracker tracker = tracker();

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(
                inputChanges(true, List.of(fileChange(applicationClass, ChangeType.ADDED))), true, inputs());

        assertThat(observed.incremental()).isFalse();
        assertThat(tracker.toBuildOutputChanges(1, observed).deliveryKind())
                .isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
    }

    @Test
    void validEmptySnapshotStillAcceptsIncrementalInput() throws Exception {
        GradleDevOutputSnapshot.captureEmpty().write(snapshotFile);
        Path applicationClass = Files.writeString(classesDirectory.resolve("Application.class"), "class");
        GradleDevOutputTracker tracker = tracker();

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(
                inputChanges(true, List.of(fileChange(applicationClass, ChangeType.ADDED))), true, inputs());

        assertThat(observed.incremental()).isTrue();
        assertThat(observed.rebaseline()).isFalse();
        assertThat(observed.changes()).singleElement()
                .satisfies(change -> assertThat(change.kind()).isEqualTo(BuildOutputChangeKind.ADDED));
        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
    }

    @Test
    void missingSnapshotForNonIncrementalReadyInputPersistsCurrentStateAndForcesRebaseline() throws Exception {
        Files.writeString(classesDirectory.resolve("Application.class"), "class");
        GradleDevOutputTracker tracker = tracker();

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(
                inputChanges(false, List.of()), true, inputs());

        assertThat(observed.incremental()).isFalse();
        assertThat(tracker.toBuildOutputChanges(2, observed).deliveryKind())
                .isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
    }

    @Test
    void corruptSnapshotForIncrementalInputPersistsCurrentStateAndForcesRebaseline() throws Exception {
        Path applicationClass = Files.writeString(classesDirectory.resolve("Application.class"), "class");
        Files.createDirectories(snapshotFile.getParent());
        Files.writeString(snapshotFile, "not-a-snapshot\n");
        GradleDevOutputTracker tracker = tracker();

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(
                inputChanges(true, List.of(fileChange(applicationClass, ChangeType.MODIFIED))), true, inputs());

        assertThat(observed.incremental()).isFalse();
        assertThat(tracker.toBuildOutputChanges(3, observed).deliveryKind())
                .isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
    }

    @Test
    void incrementalObservationPersistsBeforeAnyDeliveryOutcomeExists() throws Exception {
        Path applicationClass = Files.writeString(classesDirectory.resolve("Application.class"), "before");
        snapshot().write(snapshotFile);
        Files.writeString(applicationClass, "after");
        GradleDevOutputTracker tracker = tracker();

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(
                inputChanges(true, List.of(fileChange(applicationClass, ChangeType.MODIFIED))), true, inputs());

        assertThat(observed.incremental()).isTrue();
        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
        // A later transport failure or NOT_APPLIED response must not be mistaken for a
        // compilation failure and roll back this successfully observed Gradle output.
    }

    @Test
    void sendFailureOutcomeIsConstructedAfterSnapshotPersistenceAndBeforeReceipt() throws Exception {
        assertDeliveryOutcomeOrdering("SEND_FAILED");
    }

    @Test
    void notAppliedOutcomeIsConstructedAfterSnapshotPersistenceAndBeforeReceipt() throws Exception {
        assertDeliveryOutcomeOrdering("SENT_NOT_APPLIED");
    }

    @Test
    void incrementalRuntimeJarChangeIsCountedAndRequestsRestart() throws Exception {
        Path runtimeJar = Files.writeString(testDirectory.resolve("runtime.jar"), "jar");
        ConfigurableFileCollection runtimeJars = project.files(runtimeJar.toFile());
        GradleDevOutputTracker tracker = new GradleDevOutputTracker(
                List.of(
                        new GradleDevOutputSnapshot.Root(GradleDevOutputScope.MAIN_CLASSES, classesDirectory),
                        new GradleDevOutputSnapshot.Root(GradleDevOutputScope.RUNTIME_JARS, runtimeJar)),
                snapshotFile);
        GradleDevOutputSnapshot.captureEmpty().write(snapshotFile);
        InputChanges inputChanges = mock(InputChanges.class);
        when(inputChanges.isIncremental()).thenReturn(true);
        when(inputChanges.getFileChanges(classes)).thenReturn(List.of());
        FileChange runtimeJarChange = fileChange(runtimeJar, ChangeType.MODIFIED);
        when(inputChanges.getFileChanges(runtimeJars))
                .thenReturn(List.of(runtimeJarChange));

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(inputChanges, true, List.of(
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.MAIN_CLASSES, classes),
                new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.RUNTIME_JARS, runtimeJars)));

        assertThat(observed.runtimeJarChanges()).isEqualTo(1);
        BuildOutputChanges buildChanges = tracker.toBuildOutputChanges(5, observed);
        assertThat(buildChanges.forceRestart()).isTrue();
        QuarkusApplicationDevDeploymentHandle session = mock(QuarkusApplicationDevDeploymentHandle.class);
        when(session.acceptRestartRequiredOutcome(5)).thenReturn("RESTART_REQUIRED");

        assertThat(QuarkusApplicationDevTask.acceptChanges(session, buildChanges, true, true, 1, false))
                .isEqualTo("RESTART_REQUIRED");
        verify(session).acceptRestartRequiredOutcome(5);
        verify(session, never()).acceptReadyChangesOutcome(buildChanges);
    }

    @Test
    void nonIncrementalInputDoesNotReportRuntimeJarsAsChanges() throws Exception {
        Path runtimeJar = Files.writeString(testDirectory.resolve("runtime.jar"), "jar");
        ConfigurableFileCollection runtimeJars = project.files(runtimeJar.toFile());
        GradleDevOutputTracker tracker = new GradleDevOutputTracker(
                List.of(new GradleDevOutputSnapshot.Root(GradleDevOutputScope.RUNTIME_JARS, runtimeJar)),
                snapshotFile);
        InputChanges inputChanges = mock(InputChanges.class);
        when(inputChanges.isIncremental()).thenReturn(false);

        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(inputChanges, false,
                List.of(new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.RUNTIME_JARS, runtimeJars)));

        assertThat(observed.runtimeJarChanges()).isZero();
        verify(inputChanges, never()).getFileChanges(runtimeJars);
    }

    @Test
    void recoveryPersistsCurrentSnapshotBeforeReturningRebaseline() throws Exception {
        Files.writeString(classesDirectory.resolve("Application.class"), "class");
        GradleDevOutputTracker tracker = tracker();

        var rebaseline = tracker.recoveryRebaseline(4);

        assertThat(rebaseline.deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
    }

    @Test
    void rejectsChangedPathOutsideResolvedOutputRoots() throws Exception {
        Path outside = Files.writeString(testDirectory.resolve("Outside.class"), "class");
        GradleDevOutputTracker tracker = tracker();

        assertThatThrownBy(() -> tracker.observe(
                inputChanges(true, List.of(fileChange(outside, ChangeType.ADDED))), true, inputs()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not under a declared dev output root");
        assertThat(snapshotFile).doesNotExist();
    }

    private GradleDevOutputTracker tracker() {
        return new GradleDevOutputTracker(
                List.of(new GradleDevOutputSnapshot.Root(GradleDevOutputScope.MAIN_CLASSES, classesDirectory)),
                snapshotFile);
    }

    private GradleDevOutputSnapshot snapshot() throws Exception {
        return GradleDevOutputSnapshot.capture(
                List.of(new GradleDevOutputSnapshot.Root(GradleDevOutputScope.MAIN_CLASSES, classesDirectory)));
    }

    private List<GradleDevOutputTracker.IncrementalInput> inputs() {
        return List.of(new GradleDevOutputTracker.IncrementalInput(GradleDevOutputScope.MAIN_CLASSES, classes));
    }

    private InputChanges inputChanges(boolean incremental, List<FileChange> fileChanges) {
        InputChanges inputChanges = mock(InputChanges.class);
        when(inputChanges.isIncremental()).thenReturn(incremental);
        when(inputChanges.getFileChanges(classes)).thenReturn(fileChanges);
        return inputChanges;
    }

    private void assertDeliveryOutcomeOrdering(String deliveredOutcome) throws Exception {
        Path applicationClass = Files.writeString(classesDirectory.resolve("Application.class"), "before");
        snapshot().write(snapshotFile);
        Files.writeString(applicationClass, "after");
        Path receipt = testDirectory.resolve("receipt/dev-iteration.properties");
        GradleDevOutputTracker tracker = tracker();
        GradleDevOutputTracker.ObservedDevChanges observed = tracker.observe(
                inputChanges(true, List.of(fileChange(applicationClass, ChangeType.MODIFIED))), true, inputs());
        BuildOutputChanges buildChanges = tracker.toBuildOutputChanges(6, observed);
        QuarkusApplicationDevDeploymentHandle session = mock(QuarkusApplicationDevDeploymentHandle.class);
        when(session.acceptReadyChangesOutcome(buildChanges)).thenReturn("PENDING");
        when(session.deliverReadyChangesOutcome()).thenAnswer(ignored -> {
            assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot())).isEmpty();
            assertThat(receipt).doesNotExist();
            return deliveredOutcome;
        });

        String outcome = QuarkusApplicationDevTask.acceptChanges(session, buildChanges, true, true, 0, false);
        assertThat(outcome).isEqualTo("PENDING," + deliveredOutcome);
        QuarkusApplicationDevTask.writeReceipt(receipt, 6, true, observed.changes().size(), 0, outcome, true);

        InOrder ordered = Mockito.inOrder(session);
        ordered.verify(session).acceptReadyChangesOutcome(buildChanges);
        ordered.verify(session).deliverReadyChangesOutcome();
        assertThat(receipt).hasContent(
                "sequence=6\n"
                        + "incremental=true\n"
                        + "observedChanges=1\n"
                        + "runtimeJarChanges=0\n"
                        + "sessionReady=true\n"
                        + "outcome=PENDING," + deliveredOutcome + "\n");
        assertThat(buildChanges.status()).isEqualTo(BuildOutputChangeStatus.BUILD_SUCCEEDED);
        assertThat(buildChanges.failureKind()).isEqualTo(BuildOutputFailureKind.NONE);
        verify(session, never()).acceptRestartRequiredOutcome(buildChanges.sequence());
        verify(session, never()).acceptStartupBaselineOutcome(buildChanges);
    }

    private static FileChange fileChange(Path path, ChangeType changeType) {
        FileChange change = mock(FileChange.class);
        File file = path.toFile();
        when(change.getFile()).thenReturn(file);
        when(change.getChangeType()).thenReturn(changeType);
        return change;
    }
}
