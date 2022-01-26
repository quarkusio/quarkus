package io.quarkus.container.image.docker.deployment;

import static io.quarkus.container.image.docker.deployment.TestUtil.getPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RedHatOpenJDKRuntimeBaseProviderTest {

    private final DockerFileBaseInformationProvider sut = new RedHatOpenJDKRuntimeBaseProvider();

    @Test
    void testImageWithJava11() {
        Path path = getPath("openjdk-11-runtime");
        var result = sut.determine(path);
        assertThat(result).hasValueSatisfying(v -> {
            assertThat(v.getBaseImage()).isEqualTo("registry.access.redhat.com/ubi8/openjdk-11-runtime:1.10");
            assertThat(v.getJavaVersion()).isEqualTo(11);
        });
    }

    @Test
    void testImageWithJava17() {
        Path path = getPath("openjdk-17-runtime");
        var result = sut.determine(path);
        assertThat(result).hasValueSatisfying(v -> {
            assertThat(v.getBaseImage()).isEqualTo("registry.access.redhat.com/ubi8/openjdk-17-runtime");
            assertThat(v.getJavaVersion()).isEqualTo(17);
        });
    }

    @Test
    void testUnhandled() {
        Path path = getPath("ubi-java11");
        var result = sut.determine(path);
        assertThat(result).isEmpty();
    }

}
