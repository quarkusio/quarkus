package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * WebSocket endpoint.
 */
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface WebSocket {

    /**
     *
     * @return the path
     */
    public String value();

}
