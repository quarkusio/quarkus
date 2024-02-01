package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotated method is invoked when an incoming web socket message is received.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface OnMessage {

    /**
     *
     * @return {@code true} if all the connected clients should receive the objects emitted by the annotated method
     */
    public boolean broadcast() default false;

}
