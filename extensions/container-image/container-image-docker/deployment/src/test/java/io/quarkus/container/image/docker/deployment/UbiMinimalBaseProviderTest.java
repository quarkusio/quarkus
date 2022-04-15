package io.quarkus.container.image.docker.deployment;

import static io.quarkus.container.image.docker.deployment.TestUtil.getPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class UbiMinimalBaseProviderTest {

    private final DockerFileBaseInformationProvider sut = new UbiMinimalBaseProvider();

    @Test
    void testImageWithJava11() {
        Path path = getPath("ubi-java11");
        var result = sut.determine(path);
        assertThat(result).hasValueSatisfying(v -> {
            assertThat(v.getBaseImage()).isEqualTo("registry.access.redhat.com/ubi8/ubi-minimal:8.3");
            assertThat(v.getJavaVersion()).isEqualTo(11);
        });
    }

    @Test
    void testImageWithJava17() {
        Path path = getPath("ubi-java17");
        var result = sut.determine(path);
        assertThat(result).hasValueSatisfying(v -> {
            assertThat(v.getBaseImage()).isEqualTo("registry.access.redhat.com/ubi8/ubi-minimal");
            assertThat(v.getJavaVersion()).isEqualTo(17);
        });
    }

    @Test
    void testUnhandled() {
        Path path = getPath("openjdk-11-runtime");
        var result = sut.determine(path);
        assertThat(result).isEmpty();
    }

}
