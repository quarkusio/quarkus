package io.quarkus.websockets.next;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies an endpoint callback method parameter that should be injected with a value returned from
 * {@link WebSocketConnection#pathParam(String)}.
 * <p>
 * The parameter type must be {@link String} and the name must be defined in the relevant endpoint path, otherwise
 * the build fails.
 *
 * @see WebSocketConnection#pathParam(String)
 * @see WebSocket
 * @see WebSocketClientConnection#pathParam(String)
 * @see WebSocketClient
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PathParam {

    /**
     * Constant value for {@link #value()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * The name of the parameter. By default, the element's name is used as-is.
     *
     * @return the name of the parameter
     */
    String value() default ELEMENT_NAME;

}
