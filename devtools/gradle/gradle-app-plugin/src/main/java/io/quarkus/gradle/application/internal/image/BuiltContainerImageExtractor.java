package io.quarkus.gradle.application.internal.image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

public final class BuiltContainerImageExtractor {

    private static final String JAR_CONTAINER = "jar-container";
    private static final String NATIVE_CONTAINER = "native-container";
    private static final String CONTAINER_IMAGE = "container-image";
    private static final String PULL_REQUIRED = "pull-required";
    private static final String WORKING_DIRECTORY = "working-directory";
    private static final String OUTPUT_DIRECTORY = "output-directory";

    public Optional<BuiltContainerImage> extract(ImageExtractionRequest request) {
        Optional<ArtifactResult> selected = request.artifactResults().stream()
                .filter(result -> JAR_CONTAINER.equals(result.getType()) || NATIVE_CONTAINER.equals(result.getType()))
                .min(Comparator
                        .comparing(result -> request.target()
                                .map(target -> !target.reference().equals(metadata(result).get(CONTAINER_IMAGE)))
                                .orElse(false)));

        return selected.map(result -> image(request, result));
    }

    private static BuiltContainerImage image(ImageExtractionRequest request, ArtifactResult result) {
        Map<String, String> metadata = metadata(result);
        Optional<String> reference = Optional.ofNullable(metadata.get(CONTAINER_IMAGE))
                .or(() -> request.target().map(ContainerImageTarget::reference));
        Optional<String> digest = Optional.empty();
        Optional<String> imageId = Optional.empty();
        if (request.builder().filter(QuarkusApplicationImageBuilder.JIB::equals).isPresent()) {
            digest = readSideFile(request.jibDigestFile());
            imageId = readSideFile(request.jibImageIdFile());
        }
        return new BuiltContainerImage(
                result.getType(),
                request.builder(),
                request.pushed(),
                reference,
                digest,
                imageId,
                Optional.ofNullable(metadata.get(PULL_REQUIRED)).map(Boolean::parseBoolean),
                Optional.ofNullable(metadata.get(WORKING_DIRECTORY)),
                Optional.ofNullable(metadata.get(OUTPUT_DIRECTORY)));
    }

    private static Map<String, String> metadata(ArtifactResult result) {
        return result.getMetadata() == null ? Map.of() : result.getMetadata();
    }

    private static Optional<String> readSideFile(Optional<Path> path) {
        return path.filter(Files::exists)
                .map(BuiltContainerImageExtractor::readString)
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read container image metadata file " + path, e);
        }
    }
}
