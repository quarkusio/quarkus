package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
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
import io.quarkus.gradle.application.internal.execution.ImageOperation;
import io.quarkus.gradle.application.internal.execution.ImageRequest;
import io.quarkus.gradle.application.internal.execution.RunRequest;
import io.quarkus.gradle.application.internal.execution.StartupOptimizedImageRequest;
import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolution;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionCodec;
import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

class QuarkusApplicationStartupOptimizedImageTaskTest {

    @TempDir
    Path directory;

    @Test
    void buildConsumesAotFileAndWritesTheProviderResultReceipt() throws IOException {
        Path archive = Files.writeString(directory.resolve("app.aot"), "aot");
        RecordingBuildOperations operations = new RecordingBuildOperations();
        QuarkusApplicationStartupOptimizedImageBuildTask task = task(
                QuarkusApplicationStartupOptimizedImageBuildTask.class, operations);
        task.getArchiveType().set(QuarkusApplicationJvmStartupArchiveType.AOT);
        task.getArchiveFile().set(archive.toFile());

        task.buildStartupOptimizedImage();

        assertRequestAndReceipt(operations, ImageOperation.BUILD, archive, "example/application:1.0-aot");
    }

    @Test
    void pushConsumesSccDirectoryAndWritesTheProviderResultReceipt() throws IOException {
        Path archive = Files.createDirectories(directory.resolve("app-scc"));
        Files.writeString(archive.resolve("cache"), "scc");
        RecordingBuildOperations operations = new RecordingBuildOperations();
        QuarkusApplicationStartupOptimizedImagePushTask task = task(
                QuarkusApplicationStartupOptimizedImagePushTask.class, operations);
        task.getArchiveType().set(QuarkusApplicationJvmStartupArchiveType.SCC);
        task.getArchiveDirectory().set(archive.toFile());
        task.getImageSuffix().set("-scc");
        new ImageReferenceResolutionCodec().write(
                task.getImageReferencePreflightReceiptFile().get().getAsFile().toPath(),
                new ImageReferenceResolution("example/application:1.0-scc", List.of()));

        task.pushStartupOptimizedImage();

        assertRequestAndReceipt(operations, ImageOperation.PUSH, archive, "example/application:1.0-scc");
    }

    @Test
    void rejectsProviderReferenceThatDisagreesWithPreflightWithoutWritingReceipt() throws IOException {
        Path archive = Files.writeString(directory.resolve("app.aot"), "aot");
        RecordingBuildOperations operations = new RecordingBuildOperations();
        operations.reportedReference = "example/application:different";
        QuarkusApplicationStartupOptimizedImageBuildTask task = task(
                QuarkusApplicationStartupOptimizedImageBuildTask.class, operations);
        task.getArchiveType().set(QuarkusApplicationJvmStartupArchiveType.AOT);
        task.getArchiveFile().set(archive.toFile());

        assertThatThrownBy(task::buildStartupOptimizedImage)
                .hasMessageContaining("named build 'application'")
                .hasMessageContaining("example/application:different")
                .hasMessageContaining("example/application:1.0-aot");
        assertThat(task.getReceiptFile().get().getAsFile()).doesNotExist();
    }

    private <T extends QuarkusApplicationStartupOptimizedImageTask> T task(
            Class<T> taskType, RecordingBuildOperations operations) {
        Project project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
        T task = project.getTasks().register("optimized", taskType).get();
        task.getBuildName().set("application");
        task.getBuildType().set(QuarkusApplicationBuildType.AOT_JAR);
        task.getApplicationName().set("application");
        task.getApplicationVersion().set("1.0");
        task.getQuarkusBuildProperties().set(Map.of());
        task.getGradleBuildDirectory().set(project.getLayout().getBuildDirectory());
        task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("optimized"));
        task.getApplicationModel().set(project.getLayout().getBuildDirectory().file("app-model.dat"));
        task.getRuntimeClasspath().setFrom(List.of());
        task.getSourceDirectories().setFrom(List.of());
        task.getImageSuffix().convention("-aot");
        Path baseReceipt = directory.resolve("base-image.properties");
        new BuiltContainerImageResultCodec().write(baseReceipt,
                image(false, "example/application:1.0"));
        task.getBaseImageReceiptFile().set(baseReceipt.toFile());
        Path preflightReceipt = directory.resolve("image-reference-preflight.properties");
        new ImageReferenceResolutionCodec().write(preflightReceipt,
                new ImageReferenceResolution("example/application:1.0-aot", List.of()));
        task.getImageReferencePreflightReceiptFile().set(preflightReceipt.toFile());
        task.getReceiptFile().set(project.getLayout().getBuildDirectory().file("optimized-result.properties"));
        task.getOperations().set(operations);
        return task;
    }

    private void assertRequestAndReceipt(RecordingBuildOperations operations, ImageOperation operation,
            Path archive, String optimizedReference) {
        assertThat(operations.request.operation()).isEqualTo(operation);
        assertThat(operations.request.archive()).isEqualTo(archive);
        assertThat(operations.request.optimizedImageReference()).isEqualTo(optimizedReference);
        assertThat(operations.request.baseImage().reference()).contains("example/application:1.0");
        assertThat(new BuiltContainerImageResultCodec().read(directory.resolve("build/optimized-result.properties")))
                .isEqualTo(image(operation == ImageOperation.PUSH, optimizedReference));
    }

    private static BuiltContainerImage image(boolean pushed, String reference) {
        return new BuiltContainerImage(
                "startup-optimized",
                Optional.empty(),
                pushed,
                Optional.of(reference),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("/work"),
                Optional.empty());
    }

    private static final class RecordingBuildOperations implements BuildOperations {

        private StartupOptimizedImageRequest request;
        private String reportedReference;

        @Override
        public void build(BuildRequest request) {
        }

        @Override
        public PackageResult buildPackage(BuildRequest request, Path augmentResultFile) {
            return null;
        }

        @Override
        public NativeResult buildNative(BuildRequest request, Path augmentResultFile) {
            return null;
        }

        @Override
        public BuiltContainerImage buildStartupOptimizedImage(StartupOptimizedImageRequest request) {
            this.request = request;
            return image(false, reportedReference != null ? reportedReference : request.optimizedImageReference());
        }

        @Override
        public BuiltContainerImage pushStartupOptimizedImage(StartupOptimizedImageRequest request) {
            this.request = request;
            return image(true, reportedReference != null ? reportedReference : request.optimizedImageReference());
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
