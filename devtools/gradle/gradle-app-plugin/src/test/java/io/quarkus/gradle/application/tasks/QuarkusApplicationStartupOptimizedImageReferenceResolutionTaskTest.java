package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.internal.image.ImageReferenceClaimService;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolution;
import io.quarkus.gradle.application.internal.image.ImageReferenceResolutionCodec;

class QuarkusApplicationStartupOptimizedImageReferenceResolutionTaskTest {

    private final ImageReferenceResolutionCodec codec = new ImageReferenceResolutionCodec();

    @TempDir
    Path directory;

    @Test
    void rejectsDerivedReferenceCollisionAcrossNamedBuilds() {
        Project project = ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
        ImageReferenceClaimService claims = new ImageReferenceClaimService() {
            @Override
            public BuildServiceParameters.None getParameters() {
                return null;
            }
        };
        QuarkusApplicationStartupOptimizedImageReferenceResolutionTask first = task(
                project, claims, "first", "quay.io/acme/optimized:1", "-same");
        QuarkusApplicationStartupOptimizedImageReferenceResolutionTask second = task(
                project, claims, "second", "quay.io/acme/optimized:1-s", "ame");

        first.resolveReferences();

        assertThat(codec.read(first.getResolutionReceiptFile().get().getAsFile().toPath()))
                .isEqualTo(new ImageReferenceResolution("quay.io/acme/optimized:1-same", List.of()));
        assertThatThrownBy(second::resolveReferences)
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("Container image reference collision for 'quay.io/acme/optimized:1-same'")
                .hasMessageContaining("named build 'first'")
                .hasMessageContaining("named build 'second'")
                .hasMessageContaining("startup-optimized image");
    }

    private QuarkusApplicationStartupOptimizedImageReferenceResolutionTask task(
            Project project,
            ImageReferenceClaimService claims,
            String buildName,
            String baseReference,
            String suffix) {
        Path baseReceipt = directory.resolve(buildName + "-base.properties");
        codec.write(baseReceipt, new ImageReferenceResolution(baseReference, List.of()));
        QuarkusApplicationStartupOptimizedImageReferenceResolutionTask task = project.getTasks()
                .register(buildName + "ReferencePreflight",
                        QuarkusApplicationStartupOptimizedImageReferenceResolutionTask.class)
                .get();
        task.getBuildName().set(buildName);
        task.getOwnerProjectPath().set(":");
        task.getImageSuffix().set(suffix);
        task.getBaseResolutionReceiptFile().set(baseReceipt.toFile());
        task.getResolutionReceiptFile().set(directory.resolve(buildName + "-optimized.properties").toFile());
        task.getClaimService().set(claims);
        return task;
    }
}
