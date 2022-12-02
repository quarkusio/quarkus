package io.quarkus.platform.descriptor.loader.json;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public interface ResourceLoader {

    <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException;

    default <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        return this.loadResourceAsPath(name, p -> {
            try (InputStream is = Files.newInputStream(p)) {
                return consumer.consume(is);
            }
        });
    }

}
