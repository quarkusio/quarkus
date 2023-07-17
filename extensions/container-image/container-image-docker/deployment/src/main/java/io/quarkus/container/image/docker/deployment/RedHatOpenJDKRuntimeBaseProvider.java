package io.quarkus.container.image.docker.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Can extract information from Dockerfile that uses {@code registry.access.redhat.com/ubi8/openjdk-$d-runtime:$d.$d} as the
 * base image
 */
class RedHatOpenJDKRuntimeBaseProvider
        implements DockerFileBaseInformationProvider {

    @Override
    public Optional<DockerFileBaseInformation> determine(Path dockerFile) {
        try (Stream<String> lines = Files.lines(dockerFile)) {
            Optional<String> fromOpt = lines.filter(l -> l.startsWith("FROM")).findFirst();
            if (fromOpt.isPresent()) {
                String fromLine = fromOpt.get();
                String baseImage = fromLine.substring(4).trim();
                Pattern pattern = Pattern.compile(".*ubi8/openjdk-(\\w+)-runtime.*");
                Matcher matcher = pattern.matcher(baseImage);
                if (matcher.find()) {
                    String match = matcher.group(1);
                    try {
                        return Optional.of(new DockerFileBaseInformationProvider.DockerFileBaseInformation(baseImage,
                                Integer.parseInt(match)));
                    } catch (NumberFormatException ignored) {

                    }
                }
            }
        } catch (IOException ignored) {

        }
        return Optional.empty();
    }
}
