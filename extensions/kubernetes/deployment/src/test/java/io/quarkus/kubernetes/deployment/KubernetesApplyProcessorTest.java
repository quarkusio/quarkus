
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.KubernetesApplyProcessor.getApplicableManifests;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.spi.KubernetesManifestBuildItem;

class KubernetesApplyProcessorTest {

    private static Path mockOutputPath() {
        final Path outputPath = mock(Path.class);
        doAnswer(i -> {
            final Path resolvedPath = mock(Path.class, RETURNS_DEEP_STUBS);
            when(resolvedPath.toFile().exists()).thenReturn(true);
            when(resolvedPath.toFile().isFile()).thenReturn(true);
            when(resolvedPath.toFile().getName()).thenReturn(i.getArgument(0));
            return resolvedPath;
        }).when(outputPath).resolve(anyString());

        return outputPath;
    }

    @Test
    void getApplicableManifestsValidManifestsInPathShouldReturnNonEmptyList() {
        final Path outputPath = mockOutputPath();
        final List<KubernetesManifestBuildItem> generatedManifests = Arrays.asList(
                new KubernetesManifestBuildItem("openshift", null),
                new KubernetesManifestBuildItem("openshift", "no-extension"),
                new KubernetesManifestBuildItem("openshift", "invalid.extension"),
                new KubernetesManifestBuildItem("openshift", "valid.yaml"),
                new KubernetesManifestBuildItem("openshift", "invalid.yoml"),
                new KubernetesManifestBuildItem("openshift", "valid.corner.yml"),
                new KubernetesManifestBuildItem("openshift", "invalid.yml."),
                new KubernetesManifestBuildItem("kubernetes", "valid.yaml"),
                new KubernetesManifestBuildItem("kubernetes", "invalid.yoml"),
                new KubernetesManifestBuildItem("kubernetes", "valid.corner.yml"));

        final List<Path> result = getApplicableManifests("openshift", outputPath, generatedManifests);

        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.stream().map(Path::toFile).map(File::getName).anyMatch("valid.yaml"::equals));
        assertTrue(result.stream().map(Path::toFile).map(File::getName).anyMatch("valid.corner.yml"::equals));
    }
}
