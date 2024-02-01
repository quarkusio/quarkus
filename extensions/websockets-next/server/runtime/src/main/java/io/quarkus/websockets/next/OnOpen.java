package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotated method is invoked when the client connects to a web socket.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface OnOpen {

    /**
     * @return {@code true} if all the connected clients should receive the objects emitted by the annotated method
     */
    public boolean broadcast() default false;

}
