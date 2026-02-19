package io.quarkus.dockerfiles.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.quarkus.dockerfiles.spi.Distribution;
import io.quarkus.dockerfiles.spi.DockerfileDependencyBuildItem;
import io.quarkus.dockerfiles.spi.DockerfileKind;

public class DockerfileContentTest {

    @Test
    public void shouldRenderBasicJvmDockerfile() {
        String content = DockerfileContent.jvmBuilder()
                .from("registry.access.redhat.com/ubi8/openjdk-21:1.19")
                .applicationName("my-app")
                .outputDir(Paths.get("target"))
                .build();

        assertNotNull(content);
        assertTrue(content.contains("FROM registry.access.redhat.com/ubi8/openjdk-21:1.19"));
        assertFalse(content.contains("RUN microdnf install")); // No dependencies
    }

    @Test
    public void shouldRenderBasicNativeDockerfile() {
        String content = DockerfileContent.nativeBuilder()
                .from("registry.access.redhat.com/ubi8/ubi-minimal:8.10")
                .applicationName("my-app")
                .outputDir(Paths.get("target"))
                .build();

        assertNotNull(content);
        assertTrue(content.contains("FROM registry.access.redhat.com/ubi8/ubi-minimal:8.10"));
        assertFalse(content.contains("RUN microdnf install")); // No dependencies
    }

    @Test
    public void shouldInstallUniversalDependencies() {
        String content = DockerfileContent.jvmBuilder()
                .from("ubuntu:22.04")
                .applicationName("my-app")
                .outputDir(Paths.get("target"))
                .dependencies(new DockerfileDependencyBuildItem("curl"))
                .build();

        assertTrue(content
                .contains("RUN apt-get update && apt-get install -y curl && apt-get clean && rm -rf /var/lib/apt/lists/*"));
    }

    @Test
    public void shouldInstallDistributionSpecificDependencies() {
        DockerfileDependencyBuildItem buildTools = DockerfileDependencyBuildItem
                .forDistribution(Distribution.UBUNTU, "build-essential")
                .forDistribution(Distribution.ALPINE, "build-base")
                .build();

        String ubuntuContent = DockerfileContent.jvmBuilder()
                .from("ubuntu:22.04")
                .applicationName("my-app")
                .outputDir(Paths.get("target"))
                .dependencies(buildTools)
                .build();

        assertTrue(ubuntuContent.contains("build-essential"));
        assertFalse(ubuntuContent.contains("build-base"));

        String alpineContent = DockerfileContent.nativeBuilder()
                .from("alpine:latest")
                .applicationName("my-app")
                .outputDir(Paths.get("target"))
                .dependencies(buildTools)
                .build();

        assertTrue(alpineContent.contains("build-base"));
        assertFalse(alpineContent.contains("build-essential"));
    }

    @Test
    public void shouldValidateStrictDependencies() {
        DockerfileDependencyBuildItem ubuntuOnlyDep = DockerfileDependencyBuildItem
                .forDistribution(Distribution.UBUNTU, "build-essential")
                .build();

        // Should work with Ubuntu
        assertDoesNotThrow(() -> DockerfileContent.jvmBuilder()
                .from("ubuntu:22.04")
                .dependencies(ubuntuOnlyDep)
                .build());

        // Should fail with Alpine
        assertThrows(IllegalArgumentException.class, () -> DockerfileContent.jvmBuilder()
                .from("alpine:latest")
                .dependencies(ubuntuOnlyDep)
                .build());
    }

    @Test
    public void shouldFilterApplicableDependencies() {
        DockerfileDependencyBuildItem ubuntuDep = DockerfileDependencyBuildItem
                .forDistribution(Distribution.UBUNTU, "build-essential")
                .build();
        DockerfileDependencyBuildItem alpineDep = DockerfileDependencyBuildItem
                .forDistribution(Distribution.ALPINE, "build-base")
                .build();

        String ubuntuContent = DockerfileContent.jvmBuilder()
                .from("ubuntu:22.04")
                .applicableDependencies(ubuntuDep, alpineDep) // Both provided, should filter
                .build();

        assertTrue(ubuntuContent.contains("build-essential"));
        assertFalse(ubuntuContent.contains("build-base"));
    }

    @Test
    public void shouldRespectDockerfileKind() {
        DockerfileDependencyBuildItem jvmOnlyDep = new DockerfileDependencyBuildItem("openjdk-17-jdk", DockerfileKind.JVM);
        DockerfileDependencyBuildItem nativeOnlyDep = new DockerfileDependencyBuildItem("glibc-dev", DockerfileKind.NATIVE);

        String jvmContent = DockerfileContent.jvmBuilder()
                .from("ubuntu:22.04")
                .applicableDependencies(jvmOnlyDep, nativeOnlyDep)
                .build();

        assertTrue(jvmContent.contains("openjdk-17-jdk"));
        assertFalse(jvmContent.contains("glibc-dev"));

        String nativeContent = DockerfileContent.nativeBuilder()
                .from("ubuntu:22.04")
                .applicableDependencies(jvmOnlyDep, nativeOnlyDep)
                .build();

        assertFalse(nativeContent.contains("openjdk-17-jdk"));
        assertTrue(nativeContent.contains("glibc-dev"));
    }

    @Test
    public void shouldSkipValidationForUnknownDistribution() {
        DockerfileDependencyBuildItem ubuntuOnlyDep = DockerfileDependencyBuildItem
                .forDistribution(Distribution.UBUNTU, "build-essential")
                .build();

        // Should not throw for unknown distribution
        assertDoesNotThrow(() -> DockerfileContent.jvmBuilder()
                .from("scratch") // Unknown distribution
                .dependencies(ubuntuOnlyDep)
                .build());
    }

    @Test
    public void shouldCombineMultipleDependencies() {
        String content = DockerfileContent.jvmBuilder()
                .from("ubuntu:22.04")
                .dependencies(
                        new DockerfileDependencyBuildItem("curl"),
                        new DockerfileDependencyBuildItem("wget"))
                .build();

        assertTrue(content.contains("curl wget")); // Both packages in same RUN command
    }
}
