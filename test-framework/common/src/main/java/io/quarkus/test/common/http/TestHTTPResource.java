package io.quarkus.test.common.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field should be injected with a resource that is pre-configured
 * to use the correct test URL.
 * <p>
 * This could be a String or URL object, or some other HTTP/Websocket based client.
 * <p>
 * This mechanism is pluggable, via {@link TestHTTPResourceProvider}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestHTTPResource {

    /**
     * @return The path part of the URL
     */
    String value() default "";

    /**
     * @return If the URL should use the HTTPS protocol and SSL port
     * @deprecated use #tls instead
     */
    @Deprecated(since = "3.10", forRemoval = true)
    boolean ssl() default false;

    /**
     * @return if the url should use the management interface
     */
    boolean management() default false;

    /**
     * @return If the URL should use the HTTPS protocol and TLS port
     */
    boolean tls() default false;
}
