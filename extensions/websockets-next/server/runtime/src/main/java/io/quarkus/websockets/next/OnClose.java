package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotated method is invoked when the client disconnects from the web socket.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface OnClose {

}
