package io.quarkus.dockerfiles.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class DockerfileDependencyBuildItemTest {

    @Test
    public void shouldCreateUniversalDependency() {
        DockerfileDependencyBuildItem dependency = new DockerfileDependencyBuildItem("curl");

        assertTrue(dependency.appliesTo(DockerfileKind.JVM));
        assertTrue(dependency.appliesTo(DockerfileKind.NATIVE));
        assertTrue(dependency.appliesTo(Distribution.UBUNTU));
        assertTrue(dependency.appliesTo(Distribution.ALPINE));
        assertTrue(dependency.appliesTo(DockerfileKind.JVM, Distribution.UBUNTU));

        assertEquals("curl", dependency.getPackageNameFor(Distribution.UBUNTU).orElse(null));
        assertEquals("curl", dependency.getPackageNameFor(Distribution.ALPINE).orElse(null));
    }

    @Test
    public void shouldCreateJvmOnlyDependency() {
        DockerfileDependencyBuildItem dependency = new DockerfileDependencyBuildItem("openjdk-17-jdk", DockerfileKind.JVM);

        assertTrue(dependency.appliesTo(DockerfileKind.JVM));
        assertFalse(dependency.appliesTo(DockerfileKind.NATIVE));
        assertTrue(dependency.appliesTo(DockerfileKind.JVM, Distribution.UBUNTU));
        assertFalse(dependency.appliesTo(DockerfileKind.NATIVE, Distribution.UBUNTU));
    }

    @Test
    public void shouldCreateDistributionSpecificDependency() {
        DockerfileDependencyBuildItem dependency = DockerfileDependencyBuildItem
                .forDistribution(Distribution.UBUNTU, "build-essential")
                .forDistribution(Distribution.ALPINE, "build-base")
                .build();

        assertTrue(dependency.appliesTo(Distribution.UBUNTU));
        assertTrue(dependency.appliesTo(Distribution.ALPINE));
        assertFalse(dependency.appliesTo(Distribution.FEDORA));

        assertEquals("build-essential", dependency.getPackageNameFor(Distribution.UBUNTU).orElse(null));
        assertEquals("build-base", dependency.getPackageNameFor(Distribution.ALPINE).orElse(null));
        assertTrue(dependency.getPackageNameFor(Distribution.FEDORA).isEmpty());
    }

    @Test
    public void shouldCreateDistributionSpecificWithKinds() {
        DockerfileDependencyBuildItem dependency = DockerfileDependencyBuildItem
                .forDistribution(Distribution.UBUNTU, "openjdk-17-jdk")
                .forDistribution(Distribution.ALPINE, "openjdk17")
                .forKinds(DockerfileKind.JVM)
                .build();

        assertTrue(dependency.appliesTo(DockerfileKind.JVM, Distribution.UBUNTU));
        assertFalse(dependency.appliesTo(DockerfileKind.NATIVE, Distribution.UBUNTU));
        assertFalse(dependency.appliesTo(DockerfileKind.JVM, Distribution.FEDORA));

        assertEquals("openjdk-17-jdk", dependency.getPackageNameFor(Distribution.UBUNTU).orElse(null));
        assertEquals("openjdk17", dependency.getPackageNameFor(Distribution.ALPINE).orElse(null));
    }

    @Test
    public void shouldRejectEmptyKinds() {
        assertThrows(IllegalArgumentException.class, () -> new DockerfileDependencyBuildItem("curl", Set.of()));
    }

    @Test
    public void shouldCreateValidDistributionMapping() {
        DockerfileDependencyBuildItem dependency = DockerfileDependencyBuildItem
                .forDistribution(Distribution.UBUNTU, "curl")
                .build();

        assertTrue(dependency.appliesTo(Distribution.UBUNTU));
        assertEquals("curl", dependency.getPackageNameFor(Distribution.UBUNTU).orElse(null));
    }

    @Test
    public void shouldRejectNullPackageName() {
        assertThrows(NullPointerException.class, () -> new DockerfileDependencyBuildItem(null));
    }

    @Test
    public void shouldRejectNullKinds() {
        assertThrows(NullPointerException.class, () -> new DockerfileDependencyBuildItem("curl", (Set<DockerfileKind>) null));
    }
}
