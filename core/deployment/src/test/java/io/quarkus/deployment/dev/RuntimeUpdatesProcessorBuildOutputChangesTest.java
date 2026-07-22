package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.dev.spi.DevModeType;

class RuntimeUpdatesProcessorBuildOutputChangesTest {

    @TempDir
    Path applicationRoot;

    @Test
    void buildOutputChangesUsesEmptyCollectionsForNullLists() {
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, null, null, null, null, null, null,
                false, false);

        assertThat(changes.mainClassChanges()).isEmpty();
        assertThat(changes.mainResourceChanges()).isEmpty();
        assertThat(changes.testClassChanges()).isEmpty();
        assertThat(changes.testResourceChanges()).isEmpty();
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesRestartsForMainClassChange() {
        var restarted = new AtomicBoolean();
        var changedClasses = new AtomicReference<ClassScanResult>();
        var processor = newProcessor(restarted, new AtomicReference<>(), changedClasses);
        var classesRoot = applicationRoot.resolve("classes");
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                List.of(
                        new BuildOutputPathChange(classesRoot, classesRoot.resolve("com/acme/Foo.class"),
                                BuildOutputChangeKind.MODIFIED),
                        new BuildOutputPathChange(classesRoot, classesRoot.resolve("com/acme/Added.class"),
                                BuildOutputChangeKind.ADDED),
                        new BuildOutputPathChange(classesRoot, classesRoot.resolve("com/acme/Deleted.class"),
                                BuildOutputChangeKind.DELETED)),
                null, null, null, null, null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isTrue();

        assertThat(restarted).isTrue();
        assertThat(changedClasses.get().getChangedClassNames()).containsExactly("com.acme.Foo");
        assertThat(changedClasses.get().getAddedClassNames()).containsExactly("com.acme.Added");
        assertThat(changedClasses.get().getDeletedClassNames()).containsExactly("com.acme.Deleted");
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesRestartsForForcedMainResourceChange() {
        var restarted = new AtomicBoolean();
        var filesChanged = new AtomicReference<Set<String>>();
        var processor = newProcessor(restarted, filesChanged, new AtomicReference<>());
        var resourcesRoot = applicationRoot.resolve("resources");
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, null,
                List.of(new BuildOutputPathChange(resourcesRoot, resourcesRoot.resolve("application.properties"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, null, null, false, true);

        assertThat(processor.processBuildOutputChanges(changes)).isTrue();

        assertThat(restarted).isTrue();
        assertThat(filesChanged.get()).containsExactly("application.properties");
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesIgnoresFailedBuilds() {
        var restarted = new AtomicBoolean();
        var processor = newProcessor(restarted, new AtomicReference<>(), new AtomicReference<>());
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_FAILED, null, null, null, null,
                "compilation failed", null, false, true);

        assertThat(processor.processBuildOutputChanges(changes)).isFalse();

        assertThat(restarted).isFalse();
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesIgnoresCancelledAndSupersededBuilds() {
        for (BuildOutputChangeStatus status : List.of(BuildOutputChangeStatus.BUILD_CANCELLED,
                BuildOutputChangeStatus.BUILD_SUPERSEDED)) {
            var restarted = new AtomicBoolean();
            var processor = newProcessor(restarted, new AtomicReference<>(), new AtomicReference<>());

            assertThat(processor.processBuildOutputChanges(
                    changesWithMainClass(1, status, BuildOutputChangeKind.MODIFIED, "com/acme/Foo.class"))).isFalse();
            assertThat(processor.processBuildOutputChanges(
                    changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                            "com/acme/Foo.class")))
                    .isFalse();

            assertThat(restarted).isFalse();
        }
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesIgnoresStaleSequences() {
        var restarted = new AtomicBoolean();
        var changedClassNames = new ArrayList<Set<String>>();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (filesChanged, changedClasses) -> {
                    restarted.set(true);
                    changedClassNames.add(changedClasses.getChangedClassNames());
                }, null, null, null, new AtomicReference<>());

        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(2, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Newer.class")))
                .isTrue();
        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Older.class")))
                .isFalse();

        assertThat(restarted).isTrue();
        assertThat(changedClassNames).containsExactly(Set.of("com.acme.Newer"));
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesAdvancesSequenceForFailedBuilds() {
        var restarted = new AtomicBoolean();
        var processor = newProcessor(restarted, new AtomicReference<>(), new AtomicReference<>());

        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(2, BuildOutputChangeStatus.BUILD_FAILED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Failed.class")))
                .isFalse();
        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Older.class")))
                .isFalse();

        assertThat(restarted).isFalse();
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesAdvancesSequenceWhenLiveReloadIsDisabled() {
        var restarted = new AtomicBoolean();
        var processor = newProcessor(restarted, new AtomicReference<>(), new AtomicReference<>());
        processor.setLiveReloadEnabled(false);

        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Skipped.class")))
                .isFalse();
        processor.setLiveReloadEnabled(true);
        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Skipped.class")))
                .isFalse();

        assertThat(restarted).isFalse();
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesCurrentlyIgnoresTestOutputChanges() {
        var restarted = new AtomicBoolean();
        var processor = newProcessor(restarted, new AtomicReference<>(), new AtomicReference<>());
        var testClassesRoot = applicationRoot.resolve("test-classes");
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, null, null,
                List.of(new BuildOutputPathChange(testClassesRoot, testClassesRoot.resolve("com/acme/FooTest.class"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isFalse();

        assertThat(restarted).isFalse();
    }

    @SuppressWarnings("resource")
    @Test
    void externalBuildToolUpdateSourceMakesDoScanSkipCompilerDrivenSourceScanning() throws Exception {
        var context = new DevModeContext();
        context.setBuildUpdateSource(DevModeContext.BuildUpdateSource.EXTERNAL_BUILD_TOOL);
        var processor = new RuntimeUpdatesProcessor(applicationRoot, context, null, DevModeType.LOCAL, null, null, null,
                null, new AtomicReference<>());

        assertThat(processor.doScan(false)).isFalse();
    }

    @Test
    void constructorConnectsConfiguredBuildOutputChangesTransport() throws Exception {
        var restarted = new CountDownLatch(1);
        var changedClasses = new AtomicReference<ClassScanResult>();
        try (var server = BuildOutputChangesTransports.createTcpServer()) {
            var context = new DevModeContext();
            context.setExternalBuildOutputTransport(server.transport());
            try (var ignore = new RuntimeUpdatesProcessor(applicationRoot, context, null, DevModeType.LOCAL,
                    (files, classes) -> {
                        changedClasses.set(classes);
                        restarted.countDown();
                    }, null, null, null, new AtomicReference<>())) {
                assertThat(server.send(changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                        BuildOutputChangeKind.MODIFIED, "com/acme/Foo.class")))
                        .isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

                assertThat(restarted.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(changedClasses.get().getChangedClassNames()).containsExactly("com.acme.Foo");
            }
        }
    }

    private RuntimeUpdatesProcessor newProcessor(AtomicBoolean restarted, AtomicReference<Set<String>> filesChanged,
            AtomicReference<ClassScanResult> changedClasses) {
        return new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> {
                    restarted.set(true);
                    filesChanged.set(files);
                    changedClasses.set(classes);
                }, null, null, null, new AtomicReference<>());
    }

    private BuildOutputChanges changesWithMainClass(long sequence, BuildOutputChangeStatus status, BuildOutputChangeKind kind,
            String classFile) {
        var classesRoot = applicationRoot.resolve("classes");
        return new BuildOutputChanges(sequence, status,
                List.of(new BuildOutputPathChange(classesRoot, classesRoot.resolve(classFile), kind)), null, null, null, null,
                null, false, false);
    }

}
