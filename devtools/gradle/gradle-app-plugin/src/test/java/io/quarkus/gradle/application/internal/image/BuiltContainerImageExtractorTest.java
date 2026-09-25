package io.quarkus.gradle.application.internal.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

class BuiltContainerImageExtractorTest {

    private final BuiltContainerImageExtractor extractor = new BuiltContainerImageExtractor();

    @TempDir
    Path directory;

    @Test
    void extractsDockerMetadata() {
        var image = extractor.extract(request(QuarkusApplicationImageBuilder.DOCKER, List.of(new ArtifactResult(null,
                "jar-container", Map.of(
                        "container-image", "quay.io/acme/app:1.0",
                        "pull-required", "false",
                        "working-directory", "/work",
                        "output-directory", "target"))),
                Optional.empty(), Optional.empty())).orElseThrow();

        assertThat(image.reference()).contains("quay.io/acme/app:1.0");
        assertThat(image.pullRequired()).contains(false);
        assertThat(image.workingDirectory()).contains("/work");
        assertThat(image.outputDirectory()).contains("target");
        assertThat(image.digest()).isEmpty();
    }

    @Test
    void enrichesJibDigestAndImageIdFromSideFiles() throws Exception {
        Path digest = directory.resolve("jib-image.digest");
        Path id = directory.resolve("jib-image.id");
        Files.writeString(digest, "sha256:abc\n");
        Files.writeString(id, "image-id\n");

        var image = extractor.extract(request(QuarkusApplicationImageBuilder.JIB, List.of(new ArtifactResult(null,
                "jar-container", Map.of("container-image", "quay.io/acme/app:1.0"))),
                Optional.of(digest), Optional.of(id))).orElseThrow();

        assertThat(image.digest()).contains("sha256:abc");
        assertThat(image.imageId()).contains("image-id");
    }

    @Test
    void leavesJibDigestAndImageIdEmptyWhenSideFilesAreAbsent() {
        var image = extractor.extract(request(QuarkusApplicationImageBuilder.JIB, List.of(new ArtifactResult(null,
                "jar-container", Map.of("container-image", "quay.io/acme/app:1.0"))),
                Optional.of(directory.resolve("missing.digest")), Optional.of(directory.resolve("missing.id")))).orElseThrow();

        assertThat(image.digest()).isEmpty();
        assertThat(image.imageId()).isEmpty();
    }

    @Test
    void extractsBuildpackImageReferenceWithoutDigest() {
        var image = extractor.extract(request(QuarkusApplicationImageBuilder.BUILDPACK, List.of(new ArtifactResult(null,
                "jar-container", Map.of("container-image", "quay.io/acme/app:1.0"))),
                Optional.empty(), Optional.empty())).orElseThrow();

        assertThat(image.reference()).contains("quay.io/acme/app:1.0");
        assertThat(image.digest()).isEmpty();
    }

    @Test
    void fallsBackToModeledReferenceForOpenShiftEmptyMetadata() {
        var image = extractor.extract(request(QuarkusApplicationImageBuilder.OPENSHIFT, List.of(new ArtifactResult(null,
                "jar-container", Map.of())), Optional.empty(), Optional.empty())).orElseThrow();

        assertThat(image.reference()).contains("quay.io/acme/app:1.0");
    }

    @Test
    void doesNotInventReferenceWithoutMetadataOrModeledTarget() {
        var image = extractor.extract(new ImageExtractionRequest(
                Optional.empty(),
                Optional.empty(),
                true,
                List.of(new ArtifactResult(null, "jar-container", Map.of())),
                Optional.empty(),
                Optional.empty())).orElseThrow();

        assertThat(image.reference()).isEmpty();
        assertThat(image.builder()).isEmpty();
    }

    @Test
    void prefersArtifactMatchingModeledReference() {
        var image = extractor.extract(request(QuarkusApplicationImageBuilder.BUILDPACK, List.of(
                new ArtifactResult(null, "jar-container", Map.of("container-image", "quay.io/acme/other:1.0")),
                new ArtifactResult(null, "jar-container", Map.of("container-image", "quay.io/acme/app:1.0"))),
                Optional.empty(), Optional.empty())).orElseThrow();

        assertThat(image.reference()).contains("quay.io/acme/app:1.0");
    }

    private static ImageExtractionRequest request(QuarkusApplicationImageBuilder builder,
            List<ArtifactResult> results, Optional<Path> digest, Optional<Path> id) {
        return new ImageExtractionRequest(
                Optional.of(new ContainerImageTarget("quay.io/acme/app:1.0")),
                Optional.of(builder),
                true,
                results,
                digest,
                id);
    }
}
