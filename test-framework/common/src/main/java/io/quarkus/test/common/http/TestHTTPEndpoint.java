package io.quarkus.test.common.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this test class or method is testing a specific endpoint.
 *
 * RESTAssured will also have its base URL modified
 * so all URLS can be given relative to the root of the the provided resource class. It
 * can also be applied to {@link TestHTTPResource} fields to set the base path.
 * 
 *
 * This mechanism is pluggable, and currently supports JAX-RS endpoints, Servlets and Reactive Routes.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
public @interface TestHTTPEndpoint {

    /**
     * The HTTP endpoint that is under test. All injected URL's will point to the
     * root path of the provided resource.
     */
    Class<?> value();
}
