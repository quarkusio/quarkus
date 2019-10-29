package io.quarkus.platform.descriptor.loader.json.impl;

import java.io.IOException;
import java.io.InputStream;

public class ClassPathResourceLoader implements ResourceLoader {

    private final ClassLoader cl;

    public ClassPathResourceLoader(ClassLoader cl) {
        this.cl = cl;
    }

    @Override
    public InputStream getResourceAsStream(String name) throws IOException {
        final InputStream stream = cl.getResourceAsStream(name);
        if (stream == null) {
            throw new IOException("Failed to locate " + name + " on the classpath");
        }
        return stream;
    }
}
