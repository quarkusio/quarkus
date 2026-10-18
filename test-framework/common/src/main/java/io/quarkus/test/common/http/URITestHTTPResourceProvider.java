package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

public class URITestHTTPResourceProvider implements TestHTTPResourceProvider<URI> {
    @Override
    public Class<URI> getProvidedType() {
        return URI.class;
    }

    @Override
    public URI provide(String testUri, Field field) {
        try {
            return new URI(testUri);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
