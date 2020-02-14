package io.quarkus.platform.descriptor.loader.json;

import io.quarkus.platform.descriptor.ResourceInputStreamConsumer;
import java.io.IOException;
import java.io.InputStream;

public class ClassPathResourceLoader implements ResourceLoader {

    private final ClassLoader cl;

    public ClassPathResourceLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public ClassPathResourceLoader(ClassLoader cl) {
        this.cl = cl;
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
