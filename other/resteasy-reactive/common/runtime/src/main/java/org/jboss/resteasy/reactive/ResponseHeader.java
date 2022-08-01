package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.core.Response;

/**
 * When placed on a resource method, then RESTEasy Reactive will the specified HTTP response headers,
 * if the method completes without an exception and if it does not return {@link Response} or {@link RestResponse}.
 *
 * Furthermore, users should not depend on this annotation to set the {@code Content-Type} and {@code Content-Length}
 * headers as those as set automatically by RESTEasy Reactive.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ResponseHeader.List.class)
public @interface ResponseHeader {

    String name();

    String[] value();

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        /**
         * The {@link ResponseHeader} instances.
         *
         * @return the instances
         */
        ResponseHeader[] value();
    }
}
