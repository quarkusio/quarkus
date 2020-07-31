package io.quarkus.devtools.codestarts;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class TestCodestartResourceLoader implements CodestartResourceLoader {
    @Override
    public <T> T loadResourceAsPath(String name, Consumer<T> consumer) throws IOException {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IOException("Failed to locate " + name + " on the classpath");
        }
        try {
            return consumer.consume(new File(url.toURI()).toPath());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
