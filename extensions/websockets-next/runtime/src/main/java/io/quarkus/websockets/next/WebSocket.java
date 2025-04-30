package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

/**
 * Denotes a WebSocket server endpoint.
 * <p>
 * An endpoint must declare a method annotated with {@link OnTextMessage}, {@link OnBinaryMessage}, {@link OnPingMessage},
 * {@link OnPongMessage} or {@link OnOpen}. An endpoint may declare a method annotated with {@link OnClose}.
 *
 * <h2>Lifecycle and concurrency</h2>
 * Endpoint implementation class must be a CDI bean. If no scope annotation is defined then {@link Singleton} is used.
 * {@link ApplicationScoped} and {@link Singleton} endpoints are shared accross all WebSocket connections. Therefore,
 * implementations should be either stateless or thread-safe.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface WebSocket {

    /**
     * The path of the endpoint.
     * <p>
     * It is possible to match path parameters. The placeholder of a path parameter consists of the parameter name surrounded by
     * curly brackets. The actual value of a path parameter can be obtained using
     * {@link WebSocketConnection#pathParam(String)}. For example, the path <code>/foo/{bar}</code> defines the path
     * parameter {@code bar}.
     *
     * @see WebSocketConnection#pathParam(String)
     */
    public String path();

    /**
     * By default, the fully qualified name of the annotated class is used.
     *
     * @return the endpoint id
     * @see WebSocketConnection#endpointId()
     */
    public String endpointId() default FCQN_NAME;

    /**
     * The mode used to process incoming events for a specific connection.
     */
    public InboundProcessingMode inboundProcessingMode() default InboundProcessingMode.SERIAL;

    /**
     * Constant value for {@link #endpointId()} indicating that the fully qualified name of the annotated class should be used.
     */
    String FCQN_NAME = "<<fcqn name>>";

}
