package io.quarkus.platform.descriptor.loader.json;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.fs.util.ZipUtils;

public class ZipResourceLoader implements ResourceLoader {

    private final Path zip;

    public ZipResourceLoader(Path zip) {
        this.zip = zip;
    }

    @Override
    public <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException {
        try (FileSystem fs = ZipUtils.newFileSystem(zip)) {
            final Path path = fs.getPath("/", name);
            if (!Files.exists(path)) {
                throw new IOException("Failed to locate " + name + " in " + zip);
            }
            return consumer.consume(path);
        }
    }

}
