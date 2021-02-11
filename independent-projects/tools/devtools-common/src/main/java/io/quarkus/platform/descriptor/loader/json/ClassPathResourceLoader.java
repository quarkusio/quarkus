package io.quarkus.platform.descriptor.loader.json;

import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import io.quarkus.platform.descriptor.ResourcePathConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;

public class ClassPathResourceLoader implements ResourceLoader {

    private final ClassLoader cl;

    public ClassPathResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ClassPathResourceLoader(ClassLoader cl) {
        this.cl = cl;
    }

    @Override
    public <T> T loadResourceAsPath(String name, ResourcePathConsumer<T> consumer) throws IOException {
        final URL url = cl.getResource(name);
        if (url == null) {
            throw new IOException("Failed to locate " + name + " on the classpath");
        }
        return ResourceLoaders.processAsPath(url, is -> {
            try {
                return consumer.consume(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public <T> T loadResource(String name, ResourceInputStreamConsumer<T> consumer) throws IOException {
        final InputStream stream = cl.getResourceAsStream(name);
        if (stream == null) {
            throw new IOException("Failed to locate " + name + " on the classpath");
        }
        return consumer.consume(stream);
    }
}
