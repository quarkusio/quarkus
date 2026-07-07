package io.quarkus.gradle.application.internal.dev;

import static io.quarkus.deployment.dev.BuildOutputChangeKind.ADDED;
import static io.quarkus.deployment.dev.BuildOutputChangeKind.DELETED;
import static io.quarkus.deployment.dev.BuildOutputChangeKind.MODIFIED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_FAILED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_SUCCEEDED;
import static io.quarkus.gradle.application.internal.dev.GradleDevOutputScope.MAIN_CLASSES;
import static io.quarkus.gradle.application.internal.dev.GradleDevOutputScope.MAIN_RESOURCES;
import static io.quarkus.gradle.application.internal.dev.GradleDevOutputScope.TEST_CLASSES;
import static io.quarkus.gradle.application.internal.dev.GradleDevOutputScope.TEST_RESOURCES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.dev.BuildOutputChangeKind;
import io.quarkus.deployment.dev.BuildOutputFailureKind;
import io.quarkus.deployment.dev.BuildOutputPathChange;

class GradleDevOutputChangeMapperTest {

    @TempDir
    Path directory;

    @Test
    void normalizesPathsAndRejectsChangesOutsideOutputRoot() {
        Path root = Path.of("build/classes/../classes/java/main");
        Path changed = Path.of("build/classes/java/main/./org/acme/Foo.class");

        var change = new GradleDevFileChange(MAIN_CLASSES, root, changed, ADDED);

        assertThat(change.outputRoot()).isEqualTo(Path.of("build/classes/java/main"));
        assertThat(change.changedPath()).isEqualTo(Path.of("build/classes/java/main/org/acme/Foo.class"));
        assertThatThrownBy(() -> new GradleDevFileChange(MAIN_CLASSES, Path.of("build/classes"),
                Path.of("build/generated/Foo.class"), MODIFIED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Changed path must be under output root");
    }

    @Test
    void preservesBuildOutputChangeKinds() {
        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(7, BUILD_SUCCEEDED,
                List.of(
                        classChange("Added.class", ADDED),
                        classChange("Modified.class", MODIFIED),
                        classChange("Removed.class", DELETED)),
                null, null, true, true));

        assertThat(result.sequence()).isEqualTo(7);
        assertThat(result.status()).isEqualTo(BUILD_SUCCEEDED);
        assertThat(result.userInitiated()).isTrue();
        assertThat(result.forceRestart()).isTrue();
        assertThat(result.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(ADDED, MODIFIED, DELETED);
    }

    @Test
    void includesOnlyClassFilesFromClassOutputs() {
        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(1, BUILD_SUCCEEDED,
                List.of(
                        classChange("org/acme/Foo.class", MODIFIED),
                        classChange("org/acme/Foo.java", MODIFIED),
                        classChange("org/acme/resource.txt", ADDED)),
                null, null, false, false));

        assertThat(result.mainClassChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(Path.of("build/classes/java/main/org/acme/Foo.class"));
        assertThat(result.mainResourceChanges()).isEmpty();
    }

    @Test
    void includesOrdinaryResourceFilesAndSkipsResourceDirectories() throws IOException {
        Path root = Files.createDirectories(directory.resolve("resources/main"));
        Path file = Files.writeString(root.resolve("application.properties"), "key=value");
        Path nestedDirectory = Files.createDirectories(root.resolve("META-INF"));

        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(1, BUILD_SUCCEEDED,
                List.of(
                        resourceChange(root, file, MODIFIED),
                        resourceChange(root, nestedDirectory, ADDED),
                        resourceChange(root, root.resolve("deleted.properties"), DELETED)),
                null, null, false, false));

        assertThat(result.mainResourceChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(file, root.resolve("deleted.properties"));
        assertThat(result.mainResourceChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(MODIFIED, DELETED);
        assertThat(result.mainClassChanges()).isEmpty();
    }

    @Test
    void mapsDependencyOutputChangesAsMainChanges() {
        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(1, BUILD_SUCCEEDED,
                List.of(
                        new GradleDevFileChange(GradleDevOutputScope.DEPENDENCY_CLASSES,
                                Path.of("other/build/classes/java/main"),
                                Path.of("other/build/classes/java/main/org/acme/Library.class"), MODIFIED),
                        new GradleDevFileChange(GradleDevOutputScope.DEPENDENCY_RESOURCES,
                                Path.of("other/build/resources/main"),
                                Path.of("other/build/resources/main/library.properties"), MODIFIED)),
                null, null, false, false));

        assertThat(result.mainClassChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(Path.of("other/build/classes/java/main/org/acme/Library.class"));
        assertThat(result.mainResourceChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(Path.of("other/build/resources/main/library.properties"));
        assertThat(result.testClassChanges()).isEmpty();
        assertThat(result.testResourceChanges()).isEmpty();
    }

    @Test
    void mapsTestOutputsSeparatelyFromMainOutputs() {
        Path classesRoot = Path.of("build/classes/java/test");
        Path resourcesRoot = Path.of("build/resources/test");
        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(1, BUILD_SUCCEEDED,
                List.of(
                        new GradleDevFileChange(TEST_CLASSES, classesRoot,
                                classesRoot.resolve("org/acme/GreetingTest.class"), MODIFIED),
                        new GradleDevFileChange(TEST_RESOURCES, resourcesRoot,
                                resourcesRoot.resolve("test.properties"), DELETED)),
                null, null, false, false));

        assertThat(result.mainClassChanges()).isEmpty();
        assertThat(result.mainResourceChanges()).isEmpty();
        assertThat(result.testClassChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(classesRoot.resolve("org/acme/GreetingTest.class"));
        assertThat(result.testResourceChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(resourcesRoot.resolve("test.properties"));
    }

    @Test
    void runtimeJarChangesAreRestartMetadataNotReloadableFiles() {
        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(1, BUILD_SUCCEEDED,
                List.of(new GradleDevFileChange(GradleDevOutputScope.RUNTIME_JARS,
                        Path.of("lib/dependency.jar"), Path.of("lib/dependency.jar"), MODIFIED)),
                "Runtime jar dependency changes require restarting quarkusApplicationDev.", null, false, true));

        assertThat(result.mainClassChanges()).isEmpty();
        assertThat(result.mainResourceChanges()).isEmpty();
        assertThat(result.failureSummary()).contains("Runtime jar dependency changes");
        assertThat(result.forceRestart()).isTrue();
    }

    @Test
    void preservesFailureDiagnosticsAndDoesNotAddDependencyFields() {
        Path diagnostics = Path.of("build/reports/../reports/dev-build.txt");

        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(9, BUILD_FAILED,
                List.of(classChange("org/acme/Foo.class", MODIFIED)),
                "compile failed", diagnostics, false, false));

        assertThat(result.status()).isEqualTo(BUILD_FAILED);
        assertThat(result.failureSummary()).isEqualTo("compile failed");
        assertThat(result.diagnosticsPath()).isEqualTo(Path.of("build/reports/dev-build.txt"));
        assertThat(result.testClassChanges()).isEmpty();
        assertThat(result.testResourceChanges()).isEmpty();
    }

    @Test
    void preservesTheBuildFailureKind() {
        var result = GradleDevOutputChangeMapper.toBuildOutputChanges(new GradleDevBuildResult(9, BUILD_FAILED,
                BuildOutputFailureKind.TEST, List.of(), "test compilation failed", null, false, false));

        assertThat(result.failureKind()).isEqualTo(BuildOutputFailureKind.TEST);
    }

    @Test
    void mapperSurfaceDoesNotExposeGradleTypes() {
        assertNoGradleType(GradleDevOutputScope.class);
        assertNoGradleType(GradleDevFileChange.class);
        assertNoGradleType(GradleDevBuildResult.class);
        assertNoGradleType(GradleDevOutputChangeMapper.class);
    }

    private static GradleDevFileChange classChange(String relativePath, BuildOutputChangeKind kind) {
        Path root = Path.of("build/classes/java/main");
        return new GradleDevFileChange(MAIN_CLASSES, root, root.resolve(relativePath), kind);
    }

    private static GradleDevFileChange resourceChange(Path root, Path changedPath, BuildOutputChangeKind kind) {
        return new GradleDevFileChange(MAIN_RESOURCES, root, changedPath, kind);
    }

    private static void assertNoGradleType(Class<?> type) {
        assertThat(type.getName()).doesNotContain("org.gradle");
        for (Method method : type.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName()).doesNotContain("org.gradle");
            for (Class<?> parameterType : method.getParameterTypes()) {
                assertThat(parameterType.getName()).doesNotContain("org.gradle");
            }
        }
    }
}
