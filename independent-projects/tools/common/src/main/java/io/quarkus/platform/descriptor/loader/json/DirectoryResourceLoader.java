package io.quarkus.platform.descriptor.loader.json;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;

public class DirectoryResourceLoader implements ResourceLoader {

    private final Path dir;

    public DirectoryResourceLoader(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("Failed to locate directory " + dir);
        }
        this.dir = dir;
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        Path resource = dir.resolve(name);
        if (!Files.exists(resource)) {
            throw new IOException("Failed to locate " + resource);
        }
        if (Files.isDirectory(resource)) {
            throw new IOException("Can't open a stream for path pointing to directory " + resource);
        }
        try (InputStream is = Files.newInputStream(resource)) {
            return consumer.consume(is);
        }
    }
}
