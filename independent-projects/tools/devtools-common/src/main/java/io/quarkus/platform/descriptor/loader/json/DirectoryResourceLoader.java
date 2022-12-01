package io.quarkus.platform.descriptor.loader.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryResourceLoader implements ResourceLoader {

    private final Path dir;

    public DirectoryResourceLoader(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("Failed to locate directory " + dir);
        }
        this.dir = dir;
    }

    @Override
    public <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException {
        Path path = dir.resolve(name);
        if (!Files.exists(path)) {
            throw new IOException("Failed to locate " + name + " dir on the classpath");
        }
        return consumer.consume(path);
    }
}
