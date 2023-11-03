package io.quarkus.test.common.http;

import java.lang.reflect.Field;

public class StringTestHTTPResourceProvider implements TestHTTPResourceProvider<String> {
    @Override
    public Class<String> getProvidedType() {
        return String.class;
    }

    @Override
    public String provide(String testUri, Field field) {
        return testUri;
    }
}
