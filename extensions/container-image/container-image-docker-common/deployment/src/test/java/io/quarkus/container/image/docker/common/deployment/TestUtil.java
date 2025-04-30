package io.quarkus.container.image.docker.common.deployment;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

final class TestUtil {

    private TestUtil() {
    }

    static Path getPath(String filename) {
        try {
            return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
