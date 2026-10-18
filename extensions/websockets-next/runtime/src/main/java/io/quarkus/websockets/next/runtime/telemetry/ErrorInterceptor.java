package io.quarkus.websockets.next.runtime.telemetry;

/**
 * Error interceptor must be used to intercept
 * {@link io.quarkus.websockets.next.runtime.WebSocketEndpoint#doOnError(Throwable)}.
 * The 'doOnError' method is called from within the class and using an endpoint wrapper wouldn't be sufficient.
 */
public sealed interface ErrorInterceptor permits ErrorCountingInterceptor {

    void intercept(Throwable throwable);

}
