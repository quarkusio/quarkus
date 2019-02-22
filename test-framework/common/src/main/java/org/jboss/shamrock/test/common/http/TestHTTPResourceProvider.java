package io.quarkus.test.common.http;

import java.lang.reflect.Field;
import java.net.URI;

public interface TestHTTPResourceProvider<T> {

    Class<T> getProvidedType();

    /**
     * Create the resource to be injected into the field.
     * <p>
     * Note that there is no need to directly call set() on the field, it is only provided
     * to allow you to examine the generic type and any additional annotations.
     */
    T provide(URI testUri, Field field);

}
