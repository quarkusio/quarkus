package io.quarkus.signals.spi;

import io.quarkus.signals.SignalContext;
import io.smallrye.mutiny.Uni;

/**
 * Intercepts each receiver invocation.
 * <p>
 * Implementations must be CDI beans annotated with {@link io.smallrye.common.annotation.Identifier} to define a unique
 * identifier. The ordering of interceptors can be defined with {@link ComponentOrder}.
 * <p>
 * This SPI is called once per receiver invocation, i.e., for multicast emissions ({@code publish}) it is called for each
 * matching receiver.
 * <p>
 * There is a built-in interceptor identified by {@link #ID_REQUEST_CONTEXT} that activates a new CDI request context for each
 * receiver invocation. Custom interceptors that need access to {@code @RequestScoped} beans should be ordered after it:
 *
 * <pre>
 * &#064;Identifier("my-interceptor")
 * &#064;ComponentOrder(after = ReceiverInterceptor.ID_REQUEST_CONTEXT)
 * &#064;Singleton
 * public class MyInterceptor implements ReceiverInterceptor {
 *     // ...
 * }
 * </pre>
 *
 * @see ComponentOrder
 */
public interface ReceiverInterceptor {

    /**
     * The identifier of the built-in interceptor that activates a new CDI request context for each receiver invocation.
     */
    String ID_REQUEST_CONTEXT = "quarkus.request-context";

    /**
     * Intercepts a receiver invocation.
     * <p>
     * The interceptor must call {@link InterceptionContext#proceed()} to continue the interceptor chain and eventually invoke
     * the receiver. It may transform the result, handle errors, or skip invocation by returning a different {@link Uni}.
     *
     * @param context the interception context
     * @return a {@link Uni} that completes with the receiver's response
     */
    Uni<Object> intercept(InterceptionContext context);

    /**
     * Provides contextual information about the receiver invocation being intercepted.
     */
    interface InterceptionContext {

        /**
         * @return the receiver being invoked
         */
        Receiver<?, ?> receiver();

        /**
         * @return the signal context
         */
        SignalContext<?> signalContext();

        /**
         * Proceeds to the next interceptor in the chain, or to the actual receiver invocation if this is the last
         * interceptor.
         *
         * @return a {@link Uni} that completes with the receiver's response
         */
        Uni<Object> proceed();

    }

}
