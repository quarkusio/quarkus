package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.GradleException;
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
import io.quarkus.gradle.application.internal.image.ImageReferenceResolution;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionCodec;
import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class QuarkusApplicationImageTaskTest {

    @TempDir
    Path directory;

    @Test
    void rejectsExplicitReferenceCombinedWithRepositoryOrTag() {
        Project project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
        QuarkusApplicationImageBuildTask task = project.getTasks()
                .register("imageBuild", QuarkusApplicationImageBuildTask.class).get();
        configure(task, project);
        task.getImageReference().set("quay.io/acme/app:1");
        task.getImageRepository().set("quay.io/acme/other");

        assertThatThrownBy(task::validateImageTarget)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("image reference cannot be combined with repository or tag");

        task.getImageRepository().unset();
        task.getImageTag().set("other");

        assertThatThrownBy(task::validateImageTarget)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("image reference cannot be combined with repository or tag");
    }

    @Test
    void rejectsActualReferenceThatDisagreesWithPreflightWithoutWritingReceipt() {
        Project project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
        QuarkusApplicationImageBuildTask task = project.getTasks()
                .register("imageBuild", QuarkusApplicationImageBuildTask.class).get();
        configure(task, project);
        Path preflight = directory.resolve("preflight.properties");
        new ImageReferenceResolutionCodec().write(preflight,
                new ImageReferenceResolution("quay.io/acme/app:expected", List.of()));
        task.getImageReferencePreflightReceiptFile().set(preflight.toFile());
        Path receipt = directory.resolve("image-result.properties");
        task.getReceiptFile().set(receipt.toFile());
        task.getOperations().set(new MismatchingBuildOperations());

        assertThatThrownBy(task::buildImage)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("named build 'app'")
                .hasMessageContaining("quay.io/acme/app:actual")
                .hasMessageContaining("quay.io/acme/app:expected");
        assertThat(receipt).doesNotExist();
    }

    private static void configure(QuarkusApplicationImageBuildTask task, Project project) {
        task.getBuildName().set("app");
        task.getBuildType().set(QuarkusApplicationBuildType.FAST_JAR);
        task.getApplicationName().set("app");
        task.getApplicationVersion().set("1.0");
        task.getQuarkusBuildProperties().set(Map.of());
        task.getImageQuarkusBuildProperties().set(Map.of());
        task.getGradleBuildDirectory().set(project.getLayout().getBuildDirectory());
        task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("image"));
        task.getApplicationModel().set(project.getLayout().getBuildDirectory().file("app-model.dat"));
        task.getRuntimeClasspath().setFrom(List.of());
        task.getSourceDirectories().setFrom(List.of());
    }

    private static final class MismatchingBuildOperations implements BuildOperations {

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
            return null;
        }

        @Override
        public BuiltContainerImage pushStartupOptimizedImage(StartupOptimizedImageRequest request) {
            return null;
        }

        @Override
        public BuiltContainerImage buildImage(ImageRequest request) {
            return image();
        }

        @Override
        public BuiltContainerImage pushImage(ImageRequest request) {
            return image();
        }

        @Override
        public DeploymentResult deploy(DeploymentRequest request) {
            return null;
        }

        @Override
        public void run(RunRequest request) {
        }

        private static BuiltContainerImage image() {
            return new BuiltContainerImage(
                    "jar-container", Optional.empty(), false,
                    Optional.of("quay.io/acme/app:actual"), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty());
        }
    }
}
