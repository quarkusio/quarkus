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

import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.PathList;

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

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

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

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(restarted).isTrue();
        assertThat(filesChanged.get()).containsExactly("application.properties");
    }

    @SuppressWarnings("resource")
    @Test
    void missingFailureKindDefaultsToMainBuildFailure() {
        var restarted = new AtomicBoolean();
        var processor = newProcessor(restarted, new AtomicReference<>(), new AtomicReference<>());
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_FAILED, null, null, null, null,
                "compilation failed", null, false, true);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(restarted).isFalse();
        assertThat(processor.getCompileProblem()).isInstanceOf(ExternalBuildException.class)
                .hasMessage("compilation failed");
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesIgnoresCancelledAndSupersededBuilds() {
        for (BuildOutputChangeStatus status : List.of(BuildOutputChangeStatus.BUILD_CANCELLED,
                BuildOutputChangeStatus.BUILD_SUPERSEDED)) {
            var restarted = new AtomicBoolean();
            var processor = newProcessor(restarted, new AtomicReference<>(), new AtomicReference<>());

            assertThat(processor.processBuildOutputChanges(
                    changesWithMainClass(1, status, BuildOutputChangeKind.MODIFIED, "com/acme/Foo.class")))
                    .isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
            assertThat(processor.processBuildOutputChanges(
                    changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                            "com/acme/Foo.class")))
                    .isEqualTo(BuildOutputChangesApplyStatus.REJECTED);

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
                .isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Older.class")))
                .isEqualTo(BuildOutputChangesApplyStatus.REJECTED);

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
                .isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Older.class")))
                .isEqualTo(BuildOutputChangesApplyStatus.REJECTED);

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
                .isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        processor.setLiveReloadEnabled(true);
        assertThat(processor.processBuildOutputChanges(
                changesWithMainClass(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, BuildOutputChangeKind.MODIFIED,
                        "com/acme/Skipped.class")))
                .isEqualTo(BuildOutputChangesApplyStatus.REJECTED);

        assertThat(restarted).isFalse();
    }

    @SuppressWarnings("resource")
    @Test
    void processBuildOutputChangesRoutesTestOutputChanges() {
        var restarted = new AtomicBoolean();
        var testSupport = new RecordingTestSupport();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> restarted.set(true), null, null, testSupport, new AtomicReference<>());
        var testClassesRoot = applicationRoot.resolve("test-classes");
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, null, null,
                List.of(new BuildOutputPathChange(testClassesRoot, testClassesRoot.resolve("com/acme/FooTest.class"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(restarted).isFalse();
        assertThat(testSupport.compileSucceeded).isEqualTo(1);
        assertThat(testSupport.queuedChanges.getChangedClassNames()).containsExactly("com.acme.FooTest");
    }

    @SuppressWarnings("resource")
    @Test
    void testResourceChangesRequestAFullTestRun() {
        var testSupport = new RecordingTestSupport();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> {
                }, null, null, testSupport, new AtomicReference<>());
        Path testResources = applicationRoot.resolve("test-resources");
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED, null, null, null,
                List.of(new BuildOutputPathChange(testResources, testResources.resolve("test.properties"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(testSupport.runRequests).isEqualTo(1);
        assertThat(testSupport.queuedChanges).isNull();
    }

    @SuppressWarnings("resource")
    @Test
    void mainAndTestClassChangesAreMergedForAffectedTestSelection() {
        var testSupport = new RecordingTestSupport();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> {
                }, null, null, testSupport, new AtomicReference<>());
        Path mainClasses = applicationRoot.resolve("main-classes");
        Path testClasses = applicationRoot.resolve("test-classes");
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                List.of(new BuildOutputPathChange(mainClasses, mainClasses.resolve("com/acme/Foo.class"),
                        BuildOutputChangeKind.MODIFIED)),
                null,
                List.of(new BuildOutputPathChange(testClasses, testClasses.resolve("com/acme/FooTest.class"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(testSupport.runRequests).isEqualTo(1);
        assertThat(testSupport.queuedChanges.getChangedClassNames())
                .containsExactlyInAnyOrder("com.acme.Foo", "com.acme.FooTest");
    }

    @SuppressWarnings("resource")
    @Test
    void failedTestBuildMarksTestCompilationFailedWithoutBreakingApplication() {
        var testSupport = new RecordingTestSupport();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> {
                }, null, null, testSupport, new AtomicReference<>());
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_FAILED, BuildOutputFailureKind.TEST,
                null, null, null, null, "test compilation failed", null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(processor.compileProblem).isNull();
        assertThat(testSupport.compileFailure).isInstanceOf(ExternalBuildException.class);
    }

    @SuppressWarnings("resource")
    @Test
    void failedMainBuildBreaksApplicationAndBlocksTests() {
        var testSupport = new RecordingTestSupport();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> {
                }, null, null, testSupport, new AtomicReference<>());
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_FAILED, BuildOutputFailureKind.MAIN,
                null, null, null, null, "main compilation failed", null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(processor.compileProblem).isInstanceOf(ExternalBuildException.class)
                .hasMessage("main compilation failed");
        assertThat(testSupport.compileFailure).isInstanceOf(ExternalBuildException.class);
        assertThat(testSupport.runRequests).isZero();
    }

    @SuppressWarnings("resource")
    @Test
    void unknownFailureKindIsTreatedAsMainBuildFailure() {
        var testSupport = new RecordingTestSupport();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> {
                }, null, null, testSupport, new AtomicReference<>());
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_FAILED, BuildOutputFailureKind.UNKNOWN,
                null, null, null, null, "unknown build failure", null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(processor.getCompileProblem()).isInstanceOf(ExternalBuildException.class)
                .hasMessage("unknown build failure");
        assertThat(testSupport.compileFailure).isSameAs(processor.getCompileProblem());
    }

    @SuppressWarnings("resource")
    @Test
    void successfulBuildClearsMainAndTestBuildFailures() {
        var testSupport = new RecordingTestSupport();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, null, null, DevModeType.LOCAL,
                (files, classes) -> {
                }, null, null, testSupport, new AtomicReference<>());
        var mainFailure = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_FAILED, BuildOutputFailureKind.MAIN,
                null, null, null, null, "main compilation failed", null, false, false);
        var testFailure = new BuildOutputChanges(2, BuildOutputChangeStatus.BUILD_FAILED, BuildOutputFailureKind.TEST,
                null, null, null, null, "test compilation failed", null, false, false);
        var recovered = new BuildOutputChanges(3, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                null, null, null, null, null, null, false, false);

        assertThat(processor.processBuildOutputChanges(mainFailure)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        assertThat(processor.getCompileProblem()).isNotNull();
        assertThat(processor.processBuildOutputChanges(testFailure)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);
        assertThat(testSupport.compileFailure).isNotNull();

        assertThat(processor.processBuildOutputChanges(recovered)).isEqualTo(BuildOutputChangesApplyStatus.APPLIED);

        assertThat(processor.getCompileProblem()).isNull();
        assertThat(testSupport.compileFailure).isNull();
        assertThat(testSupport.compileSucceeded).isEqualTo(1);
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

    @SuppressWarnings("resource")
    @Test
    void rejectsChangesWhoseOutputRootWasNotDeclaredAtLaunch() {
        Path classes = applicationRoot.resolve("classes");
        var context = new DevModeContext();
        context.setApplicationRoot(new DevModeContext.ModuleInfo.Builder()
                .setArtifactKey(ArtifactKey.of("org.acme", "app"))
                .setProjectDirectory(applicationRoot.toString())
                .setSourcePaths(PathList.of())
                .setClassesPaths(List.of(classes, applicationRoot.resolve("kotlin-classes")))
                .setResourcePaths(PathList.of())
                .build());
        var restarted = new AtomicBoolean();
        var processor = new RuntimeUpdatesProcessor(applicationRoot, context, null, DevModeType.LOCAL,
                (files, changedClasses) -> restarted.set(true), null, null, null, new AtomicReference<>());
        Path unknown = applicationRoot.resolve("unknown");
        var changes = new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                List.of(new BuildOutputPathChange(unknown, unknown.resolve("org/acme/Foo.class"),
                        BuildOutputChangeKind.MODIFIED)),
                null, null, null, null, null, false, false);

        assertThat(processor.processBuildOutputChanges(changes)).isEqualTo(BuildOutputChangesApplyStatus.REJECTED);
        assertThat(restarted).isFalse();
        assertThat(context.getApplicationRoot().getMain().getClassesPaths())
                .containsExactly(classes, applicationRoot.resolve("kotlin-classes"));
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

    private static final class RecordingTestSupport extends TestSupport {

        private int compileSucceeded;
        private Throwable compileFailure;
        private int runRequests;
        private ClassScanResult queuedChanges;

        private RecordingTestSupport() {
            super(null, List.of(), null, DevModeType.LOCAL);
        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public synchronized void testCompileSucceeded() {
            compileSucceeded++;
            compileFailure = null;
        }

        @Override
        public void testCompileFailed(Throwable failure) {
            compileFailure = failure;
        }

        @Override
        public void runTests(ClassScanResult classScanResult) {
            runRequests++;
            queuedChanges = classScanResult;
        }
    }

}
