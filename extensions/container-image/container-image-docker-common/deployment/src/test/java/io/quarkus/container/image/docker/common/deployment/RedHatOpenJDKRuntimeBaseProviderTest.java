package io.quarkus.container.image.docker.common.deployment;

import static io.quarkus.container.image.docker.common.deployment.TestUtil.getPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RedHatOpenJDKRuntimeBaseProviderTest {

    private final DockerFileBaseInformationProvider sut = new RedHatOpenJDKRuntimeBaseProvider();

    @Test
    void testImageWithJava17() {
        Path path = getPath("openjdk-17-runtime");
        var result = sut.determine(path);
        assertThat(result).hasValueSatisfying(v -> {
            assertThat(v.baseImage()).isEqualTo("registry.access.redhat.com/ubi8/openjdk-17-runtime:1.19");
            assertThat(v.javaVersion()).isEqualTo(17);
        });
    }

    @Test
    void testImageWithJava21() {
        Path path = getPath("openjdk-21-runtime");
        var result = sut.determine(path);
        assertThat(result).hasValueSatisfying(v -> {
            assertThat(v.baseImage()).isEqualTo("registry.access.redhat.com/ubi8/openjdk-21-runtime:1.19");
            assertThat(v.javaVersion()).isEqualTo(21);
        });
    }

    @Test
    void testUnhandled() {
        Path path = getPath("ubi-java17");
        var result = sut.determine(path);
        assertThat(result).isEmpty();
    }

}
