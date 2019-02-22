package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.URI;

public class URITestHTTPResourceProvider implements TestHTTPResourceProvider<URI> {
    @Override
    public Class<URI> getProvidedType() {
        return URI.class;
    }

    @Override
    public URI provide(URI testUri, Field field) {
        return testUri;
    }
}
