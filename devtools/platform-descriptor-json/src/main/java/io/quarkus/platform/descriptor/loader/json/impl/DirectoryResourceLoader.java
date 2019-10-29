package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;
import java.io.InputStream;
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
    public InputStream getResourceAsStream(String name) throws IOException {
        Path resource = dir.resolve(name);
        if (!Files.exists(resource)) {
            throw new IOException("Failed to locate " + resource);
        }
        if (Files.isDirectory(resource)) {
            throw new IOException("Can't open a stream for path pointing to directory " + resource);
        }
        return Files.newInputStream(resource);
    }
}
