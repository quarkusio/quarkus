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

class RedHatOpenJDKRuntimeBaseProviderTest {

    private final DockerFileBaseInformationProvider sut = new RedHatOpenJDKRuntimeBaseProvider();

    @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER
            + ")")
    @MethodSource("imageCombinations")
    void testImage(int javaVersion, int ubiVersion, String imageVersion) {
        var path = getPath("ubi%d-openjdk-%d-runtime".formatted(ubiVersion, javaVersion));
        var result = sut.determine(path);
        assertThat(result)
                .isNotNull()
                .get()
                .extracting(
                        DockerFileBaseInformation::baseImage,
                        DockerFileBaseInformation::javaVersion)
                .containsExactly(
                        "registry.access.redhat.com/ubi%d/openjdk-%d-runtime:%s".formatted(ubiVersion, javaVersion,
                                imageVersion),
                        javaVersion);
    }

    static Stream<Arguments> imageCombinations() {
        return Stream.of(
                Arguments.of(17, 8, ContainerImages.UBI8_JAVA_VERSION),
                Arguments.of(21, 8, ContainerImages.UBI8_JAVA_VERSION),
                Arguments.of(17, 9, ContainerImages.UBI8_JAVA_VERSION),
                Arguments.of(21, 9, ContainerImages.UBI8_JAVA_VERSION));
    }

    @Test
    void testUnhandled() {
        Path path = getPath("ubi8-java17");
        var result = sut.determine(path);
        assertThat(result).isEmpty();
    }

}
