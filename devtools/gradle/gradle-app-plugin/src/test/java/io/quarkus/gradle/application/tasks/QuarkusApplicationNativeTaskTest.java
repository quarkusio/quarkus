package io.quarkus.gradle.application.tasks;

import static io.quarkus.gradle.testing.BaseGradleTest.canonicalPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.internal.deployment.DeploymentResult;
import io.quarkus.gradle.application.internal.execution.BuildOperations;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.internal.execution.DeploymentRequest;
import io.quarkus.gradle.application.internal.execution.ImageRequest;
import io.quarkus.gradle.application.internal.execution.RunRequest;
import io.quarkus.gradle.application.internal.execution.StartupOptimizedImageRequest;
import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.nativeimage.NativeResultCodec;
import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class QuarkusApplicationNativeTaskTest {

    @TempDir
    Path directory;

    @Test
    void nativeExecutableDelegatesToBuildOperationsAndWritesReceipt() {
        Path outputRoot = directory.resolve("build/quarkus-builds/native/package");
        Path executable = outputRoot.resolve("application-runner");
        NativeResult result = new NativeResult(
                "native",
                QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                outputRoot,
                "application",
                Optional.of(executable),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of("java-version", "21"),
                List.of(new NativeResult.Artifact(Optional.of(executable), "native", Map.of())));
        RecordingBuildOperations operations = new RecordingBuildOperations(result);
        QuarkusApplicationNativeTask task = nativeTask("native", QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                operations);
        task.getNativeArguments().put("quarkus.native.additional-build-args", "-O2");

        task.buildNativeImage();

        assertThat(operations.request.descriptor().name()).isEqualTo("native");
        assertThat(operations.request.descriptor().type()).isEqualTo(QuarkusApplicationBuildType.NATIVE_EXECUTABLE);
        assertThat(canonicalPath(operations.request.outputRoot())).isEqualTo(canonicalPath(outputRoot));
        assertThat(operations.request.operationForcedProperties())
                .containsExactly(Map.entry("quarkus.native.additional-build-args", "-O2"));
        assertThat(operations.request.effectiveConfig().fullValues())
                .containsEntry("quarkus.native.enabled", "true")
                .containsEntry("quarkus.native.sources-only", "false")
                .containsEntry("quarkus.native.additional-build-args", "-O2");
        assertThat(canonicalPath(operations.augmentResultFile))
                .isEqualTo(canonicalPath(directory.resolve(
                        "build/quarkus-build-results/native/package/native-augmentation-result.properties")));
        assertThat(new NativeResultCodec().read(nativeResultFile("native"))).isEqualTo(result);
    }

    @Test
    void nativeSourcesDelegatesToBuildOperationsAndWritesReceipt() {
        Path outputRoot = directory.resolve("build/quarkus-builds/sources/package");
        Path sourcesDirectory = outputRoot.resolve("native-sources");
        Path sourceJar = outputRoot.resolve("native-image-source-jar/application.jar");
        Path nativeImageArgs = sourcesDirectory.resolve("native-image.args");
        NativeResult result = new NativeResult(
                "sources",
                QuarkusApplicationBuildType.NATIVE_SOURCES,
                outputRoot,
                "application",
                Optional.empty(),
                Optional.of(sourcesDirectory),
                Optional.of(sourceJar),
                Optional.of(nativeImageArgs),
                Map.of(),
                List.of(new NativeResult.Artifact(Optional.of(sourceJar), "native-sources", Map.of())));
        RecordingBuildOperations operations = new RecordingBuildOperations(result);
        QuarkusApplicationNativeTask task = nativeTask("sources", QuarkusApplicationBuildType.NATIVE_SOURCES,
                operations);

        task.buildNativeImage();

        assertThat(operations.request.descriptor().name()).isEqualTo("sources");
        assertThat(operations.request.descriptor().type()).isEqualTo(QuarkusApplicationBuildType.NATIVE_SOURCES);
        assertThat(canonicalPath(operations.request.outputRoot())).isEqualTo(canonicalPath(outputRoot));
        assertThat(operations.request.operationForcedProperties()).isEmpty();
        assertThat(operations.request.effectiveConfig().fullValues())
                .containsEntry("quarkus.native.enabled", "true")
                .containsEntry("quarkus.native.sources-only", "true");
        assertThat(canonicalPath(operations.augmentResultFile))
                .isEqualTo(canonicalPath(directory.resolve(
                        "build/quarkus-build-results/sources/package/native-augmentation-result.properties")));
        assertThat(new NativeResultCodec().read(nativeResultFile("sources"))).isEqualTo(result);
    }

    private QuarkusApplicationNativeTask nativeTask(String buildName, QuarkusApplicationBuildType buildType,
            RecordingBuildOperations operations) {
        Project project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
        QuarkusApplicationNativeTask task = project.getTasks()
                .register("quarkus" + buildName + "Build", QuarkusApplicationNativeTask.class)
                .get();
        task.getBuildName().set(buildName);
        task.getBuildType().set(buildType);
        task.getApplicationName().set("application");
        task.getApplicationVersion().set("1.0");
        task.getOutputName().set("application");
        task.getQuarkusBuildProperties().convention(Map.of());
        task.getNativeArguments().convention(Map.of());
        task.getGradleBuildDirectory().set(project.getLayout().getBuildDirectory());
        task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                .dir("quarkus-builds/" + buildName + "/package"));
        task.getApplicationModel().set(project.getLayout().getBuildDirectory().file("app-model.dat"));
        task.getNativeResultFile().set(project.getLayout().getBuildDirectory()
                .file("quarkus-build-results/" + buildName + "/package/native-result.properties"));
        task.getRuntimeClasspath().setFrom(List.of());
        task.getSourceDirectories().setFrom(List.of());
        task.getGradlePropertyPrefixes().convention(List.of());
        task.getGradlePropertyNames().convention(List.of());
        task.getSystemPropertyPrefixes().convention(List.of());
        task.getSystemPropertyNames().convention(List.of());
        task.getEnvironmentVariablePrefixes().convention(List.of());
        task.getEnvironmentVariableNames().convention(List.of());
        task.getOperations().set(operations);
        return task;
    }

    private Path nativeResultFile(String buildName) {
        return directory.resolve("build/quarkus-build-results/" + buildName + "/package/native-result.properties");
    }

    private static final class RecordingBuildOperations implements BuildOperations {

        private final NativeResult result;
        private BuildRequest request;
        private Path augmentResultFile;

        private RecordingBuildOperations(NativeResult result) {
            this.result = result;
        }

        @Override
        public void build(BuildRequest request) {
        }

        @Override
        public PackageResult buildPackage(BuildRequest request, Path augmentResultFile) {
            return null;
        }

        @Override
        public NativeResult buildNative(BuildRequest request, Path augmentResultFile) {
            this.request = request;
            this.augmentResultFile = augmentResultFile;
            return result;
        }

        @Override
        public BuiltContainerImage buildStartupOptimizedImage(StartupOptimizedImageRequest request) {
            return null;
        }

        @Override
        public BuiltContainerImage pushStartupOptimizedImage(StartupOptimizedImageRequest request) {
            return null;
        }

        @Override
        public BuiltContainerImage buildImage(ImageRequest request) {
            return null;
        }

        @Override
        public BuiltContainerImage pushImage(ImageRequest request) {
            return null;
        }

        @Override
        public DeploymentResult deploy(DeploymentRequest request) {
            return null;
        }

        @Override
        public void run(RunRequest request) {
        }
    }
}
