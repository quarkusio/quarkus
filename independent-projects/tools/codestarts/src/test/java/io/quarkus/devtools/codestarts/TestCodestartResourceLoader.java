package io.quarkus.devtools.codestarts;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class TestCodestartResourceLoader implements CodestartPathLoader {

    @Override
    public <T> T loadResourceAsPath(String name, PathConsumer<T> consumer) throws IOException {
        return consumer.consume(getResource(name));
    }

    public static Path getResource(String name) throws IOException {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IOException("Failed to locate " + name + " on the classpath");
        }
        try {
            return new File(url.toURI()).toPath();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
