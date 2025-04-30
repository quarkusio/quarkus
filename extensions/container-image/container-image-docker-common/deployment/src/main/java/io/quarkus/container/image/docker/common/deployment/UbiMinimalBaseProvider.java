package io.quarkus.container.image.docker.common.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Can extract information from Dockerfile that uses {@code registry.access.redhat.com/ubi([8-9]|[1-9][0-9]+)/ubi-minimal:$d.$d}
 * as the
 * base image
 */
class UbiMinimalBaseProvider implements DockerFileBaseInformationProvider {
    private static final Pattern BASE_IMAGE_PATTERN = Pattern.compile(".*/ubi([8-9]|[1-9][0-9]+)/ubi-minimal");
    private static final Pattern JAVA_VERSION_PATTERN = Pattern
            .compile("ARG JAVA_PACKAGE=java-(\\w+)-openjdk-headless");

    private enum State {
        FROM_NOT_ENCOUNTERED,
        MATCHING_FROM_FOUND,
        ARG_JAVA_PACKAGE_FOUND,
        NON_MATCHING_FROM_FOUND,
        EXCEPTION_OCCURRED
    }

    @Override
    public Optional<DockerFileBaseInformation> determine(Path dockerFile) {
        AtomicInteger state = new AtomicInteger(State.FROM_NOT_ENCOUNTERED.ordinal()); //0: 'FROM' not yet encountered, 1: matching 'FROM' found, 2: ARG JAVA_PACKAGE found, 3: non matching 'FROM' found, 4: exception occurred
        AtomicReference<String> baseImage = new AtomicReference<>(null);
        AtomicInteger javaVersion = new AtomicInteger(0);
        try (Stream<String> lines = Files.lines(dockerFile)) {
            lines.takeWhile(s -> state.get() < State.ARG_JAVA_PACKAGE_FOUND.ordinal()).forEach(s -> {
                if (s.startsWith("FROM")) {
                    String image = s.substring(4).trim();
                    Matcher matcher = BASE_IMAGE_PATTERN.matcher(image);
                    if (matcher.find()) {
                        baseImage.set(image);
                        state.set(State.MATCHING_FROM_FOUND.ordinal());
                    } else {
                        state.set(State.NON_MATCHING_FROM_FOUND.ordinal());
                    }
                } else if (s.startsWith("ARG JAVA_PACKAGE")) {
                    Matcher matcher = JAVA_VERSION_PATTERN.matcher(s);
                    if (matcher.find()) {
                        String match = matcher.group(1);
                        try {
                            javaVersion.set(Integer.parseInt(match));
                            state.set(State.ARG_JAVA_PACKAGE_FOUND.ordinal());
                        } catch (NumberFormatException ignored) {
                            state.set(State.EXCEPTION_OCCURRED.ordinal());
                        }
                    }
                }
            });
        } catch (IOException ignored) {
            state.set(State.EXCEPTION_OCCURRED.ordinal());
        }

        if (state.get() == State.ARG_JAVA_PACKAGE_FOUND.ordinal()) {
            return Optional.of(new DockerFileBaseInformation(baseImage.get(), javaVersion.get()));
        }
        return Optional.empty();
    }
}
