package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.URI;

public class StringTestHTTPResourceProvider implements TestHTTPResourceProvider<String> {
    @Override
    public Class<String> getProvidedType() {
        return String.class;
    }

    @Override
    public String provide(URI testUri, Field field) {
        return testUri.toASCIIString();
    }
}
