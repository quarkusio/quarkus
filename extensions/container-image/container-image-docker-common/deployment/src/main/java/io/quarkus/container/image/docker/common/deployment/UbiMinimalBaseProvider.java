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
 * Can extract information from Dockerfile that uses {@code registry.access.redhat.com/ubi8/ubi-minimal:$d.$d} as the
 * base image
 */
class UbiMinimalBaseProvider implements DockerFileBaseInformationProvider {

    public static final String UBI_MINIMAL_PREFIX = "registry.access.redhat.com/ubi8/ubi-minimal";

    @Override
    public Optional<DockerFileBaseInformation> determine(Path dockerFile) {
        AtomicInteger state = new AtomicInteger(0); //0: 'FROM' not yet encountered, 1: matching 'FROM' found, 2: ARG JAVA_PACKAGE found, 3: non matching 'FROM' found, 4: exception occurred
        AtomicReference<String> baseImage = new AtomicReference<>(null);
        AtomicInteger javaVersion = new AtomicInteger(0);
        try (Stream<String> lines = Files.lines(dockerFile)) {
            lines.takeWhile(s -> state.get() < 2).forEach(s -> {
                if (s.startsWith("FROM")) {
                    String image = s.substring(4).trim();
                    if (image.startsWith(UBI_MINIMAL_PREFIX)) {
                        baseImage.set(image);
                        state.set(1);
                    } else {
                        state.set(3);
                    }
                } else if (s.startsWith("ARG JAVA_PACKAGE")) {
                    Pattern pattern = Pattern.compile("ARG JAVA_PACKAGE=java-(\\w+)-openjdk-headless");
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        String match = matcher.group(1);
                        try {
                            javaVersion.set(Integer.parseInt(match));
                            state.set(2);
                        } catch (NumberFormatException ignored) {
                            state.set(4);
                        }
                    }
                }
            });
        } catch (IOException ignored) {
            state.set(4);
        }
        if (state.get() == 2) {
            return Optional.of(new DockerFileBaseInformation(baseImage.get(), javaVersion.get()));
        }
        return Optional.empty();
    }
}
