package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class URLTestHTTPResourceProvider implements TestHTTPResourceProvider<URL> {
    @Override
    public Class<URL> getProvidedType() {
        return URL.class;
    }

    @Override
    public URL provide(URI testUri, Field field) {
        try {
            return testUri.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
