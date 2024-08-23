package io.quarkus.container.image.docker.common.deployment;

import static io.quarkus.container.image.docker.common.deployment.TestUtil.getPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedTest.DISPLAY_NAME_PLACEHOLDER;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.container.image.docker.common.deployment.DockerFileBaseInformationProvider.DockerFileBaseInformation;
import io.quarkus.deployment.images.ContainerImages;

class UbiMinimalBaseProviderTest {

    private final DockerFileBaseInformationProvider sut = new UbiMinimalBaseProvider();

    @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER
            + ")")
    @MethodSource("imageCombinations")
    void testImage(int ubiVersion, int javaVersion, String imageVersion) {
        var path = getPath("ubi%d-java%d".formatted(ubiVersion, javaVersion));
        var result = sut.determine(path);
        assertThat(result)
                .isNotNull()
                .get()
                .extracting(
                        DockerFileBaseInformation::baseImage,
                        DockerFileBaseInformation::javaVersion)
                .containsExactly(
                        "registry.access.redhat.com/ubi%d/ubi-minimal:%s".formatted(ubiVersion, imageVersion),
                        javaVersion);
    }

    static Stream<Arguments> imageCombinations() {
        return Stream.of(
                Arguments.of(8, 17, ContainerImages.UBI8_MINIMAL_VERSION),
                Arguments.of(8, 21, ContainerImages.UBI8_MINIMAL_VERSION),
                Arguments.of(9, 17, ContainerImages.UBI9_MINIMAL_VERSION),
                Arguments.of(9, 21, ContainerImages.UBI9_MINIMAL_VERSION));
    }

    @Test
    void testUnhandled() {
        Path path = getPath("ubi8-openjdk-17-runtime");
        var result = sut.determine(path);
        assertThat(result).isEmpty();
    }

}
