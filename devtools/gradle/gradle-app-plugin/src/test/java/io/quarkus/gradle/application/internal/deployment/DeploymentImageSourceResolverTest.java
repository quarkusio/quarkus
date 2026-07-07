package io.quarkus.gradle.application.internal.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.Optional;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.image.BuiltContainerImageResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

class DeploymentImageSourceResolverTest {

    @TempDir
    Path directory;

    private final DeploymentImageSourceResolver resolver = new DeploymentImageSourceResolver();

    @Test
    void existingImageUsesExplicitReferenceAndDisablesImageBuildAndPush() {
        var result = resolver.resolve(new DeploymentImageSourceRequest(
                QuarkusApplicationDeploymentImageSource.EXISTING_IMAGE,
                Optional.of("registry/app:1"),
                Optional.empty(),
                Optional.empty()));

        assertThat(result.imageReference()).isEqualTo("registry/app:1");
        assertThat(result.forcedProperties())
                .containsEntry("quarkus.container-image.image", "registry/app:1")
                .containsEntry("quarkus.container-image.build", "false")
                .containsEntry("quarkus.container-image.push", "false");
    }

    @Test
    void normalImagePushReadsReceiptReference() {
        Path receipt = directory.resolve("image-result.properties");
        new BuiltContainerImageResultCodec().write(receipt,
                new BuiltContainerImage("jar-container", Optional.of(QuarkusApplicationImageBuilder.JIB), true,
                        Optional.of("registry/app:1"), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()));

        var result = resolver.resolve(new DeploymentImageSourceRequest(
                QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH,
                Optional.empty(),
                Optional.of(receipt),
                Optional.empty()));

        assertThat(result.imageReference()).isEqualTo("registry/app:1");
    }

    @Test
    void normalImagePushFailsForMissingReceipt() {
        assertThatThrownBy(() -> resolver.resolve(new DeploymentImageSourceRequest(
                QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH,
                Optional.empty(),
                Optional.of(directory.resolve("missing.properties")),
                Optional.empty())))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void startupOptimizedImagePushReadsReceiptReference() {
        Path receipt = directory.resolve("startup-optimized-image-result.properties");
        new BuiltContainerImageResultCodec().write(receipt,
                new BuiltContainerImage("startup-optimized-container-image",
                        Optional.of(QuarkusApplicationImageBuilder.JIB), true,
                        Optional.of("registry/app:1-aot"), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.of("/work"), Optional.empty()));

        var result = resolver.resolve(new DeploymentImageSourceRequest(
                QuarkusApplicationDeploymentImageSource.STARTUP_OPTIMIZED_IMAGE_PUSH,
                Optional.empty(),
                Optional.empty(),
                Optional.of(receipt)));

        assertThat(result.imageReference()).isEqualTo("registry/app:1-aot");
    }
}
