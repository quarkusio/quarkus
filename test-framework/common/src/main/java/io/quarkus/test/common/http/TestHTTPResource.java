package io.quarkus.test.common.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field should be injected with a resource that is pre-configured
 * to use the correct test URL.
 *
 * This could be a String or URL object, or some other HTTP/Websocket based client.
 *
 * This mechanism is plugable, via {@link TestHTTPResourceProvider}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestHTTPResource {

    /**
     *
     * @return The path part of the URL
     */
    String value() default "";

    /**
     *
     * @return If the URL should use the HTTPS protocol and SSL port
     */
    boolean ssl() default false;
}
