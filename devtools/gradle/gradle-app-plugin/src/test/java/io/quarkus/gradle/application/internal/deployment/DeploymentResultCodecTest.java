package io.quarkus.gradle.application.internal.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

class DeploymentResultCodecTest {

    @TempDir
    Path directory;

    private final DeploymentResultCodec codec = new DeploymentResultCodec();

    @Test
    void writesDeterministicReceiptAndRoundTripsAllFields() throws Exception {
        Path receipt = directory.resolve("deployment-result.properties");

        codec.write(receipt, new DeploymentResult(
                "app",
                "prod",
                QuarkusApplicationDeploymentTarget.OPENSHIFT,
                QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH,
                "quay.io/acme/app:1.0",
                Optional.of("openshift"),
                Optional.of("openshift"),
                Optional.of("my-app"),
                Map.of("app.kubernetes.io/name", "my-app"),
                true));

        assertThat(Files.readString(receipt)).isEqualTo("""
                build.name=app
                deployment.name=prod
                deployment.target=openshift
                image.reference=quay.io/acme/app\\:1.0
                image.source=NORMAL_IMAGE_PUSH
                quarkus.deploy.target=openshift
                quarkus.kubernetes.deployment-target=openshift
                result.labels.app.kubernetes.io/name=my-app
                result.name=my-app
                schema.version=1
                success=true
                """);
        DeploymentResult result = codec.read(receipt);
        assertThat(result.buildName()).isEqualTo("app");
        assertThat(result.deploymentName()).isEqualTo("prod");
        assertThat(result.target()).isEqualTo(QuarkusApplicationDeploymentTarget.OPENSHIFT);
        assertThat(result.resultLabels()).containsEntry("app.kubernetes.io/name", "my-app");
    }

    @Test
    void rejectsUnsupportedSchemaVersion() throws Exception {
        Path receipt = directory.resolve("deployment-result.properties");
        Files.writeString(receipt, """
                schema.version=2
                build.name=app
                deployment.name=prod
                deployment.target=openshift
                image.source=NORMAL_IMAGE_PUSH
                image.reference=quay.io/acme/app\\:1.0
                success=true
                """);

        assertThatThrownBy(() -> codec.read(receipt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported schema version");
    }
}
