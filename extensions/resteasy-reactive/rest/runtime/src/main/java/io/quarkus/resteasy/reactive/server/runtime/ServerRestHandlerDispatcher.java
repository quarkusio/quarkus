package io.quarkus.resteasy.reactive.server.runtime;

import org.jboss.resteasy.reactive.server.spi.HandlerKindResolver;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Invokes the handlers of a handler chain on behalf of {@link QuarkusResteasyReactiveRequestContext},
 * avoiding the megamorphic {@link ServerRestHandler#handle} call that a plain interface call would result in.
 * <p>
 * The implementation is generated at build time from the handler classes registered via
 * {@code KnownServerRestHandlerBuildItem}. Each registered class is assigned a kind, which is simply its position
 * in the sorted list of registered classes, so a kind identifies exactly one concrete handler class and has
 * no meaning outside the generated implementation.
 * <p>
 * The kinds are used in two steps:
 * <ul>
 * <li>{@link #kindOf(ServerRestHandler)} is invoked once per handler when a handler chain is built at deployment,
 * and the resulting kinds are stored alongside the chain.
 * Handlers that were not registered get {@link HandlerKindResolver#UNKNOWN}.</li>
 * <li>{@link #dispatch(int, ServerRestHandler, QuarkusResteasyReactiveRequestContext)} is invoked for every handler
 * of every request. It is a {@code switch} on the kind with one case per registered class, in which the handler
 * is cast to that class and {@code handle} is invoked on it. Each case is thus a separate call site with a statically
 * known receiver type, which allows the JIT to devirtualize and inline the call.
 * The {@code switch} compiles to a {@code tableswitch}, so the cost of dispatching is the same for every kind
 * and the order of the kinds is irrelevant.
 * Handlers with {@link HandlerKindResolver#UNKNOWN} kind fall into the default case, which is a regular
 * (megamorphic) interface call.</li>
 * </ul>
 * Only the chains used to process a request normally carry kinds. The abort chains that are used when an exception
 * occurs do not, so all their handlers are invoked via the default case.
 * <p>
 * For example, if the registered handler classes are {@code AbortChainHandler}, {@code BlockingHandler} and
 * {@code ClassRoutingHandler}, the generated class is equivalent to:
 *
 * <pre>{@code
 * public final class ServerRestHandlerDispatcher$Generated extends ServerRestHandlerDispatcher {
 *
 *     public byte kindOf(ServerRestHandler handler) {
 *         if (handler instanceof AbortChainHandler) {
 *             return 1;
 *         }
 *         if (handler instanceof BlockingHandler) {
 *             return 2;
 *         }
 *         if (handler instanceof ClassRoutingHandler) {
 *             return 3;
 *         }
 *         return UNKNOWN;
 *     }
 *
 *     public void dispatch(int kind, ServerRestHandler handler, QuarkusResteasyReactiveRequestContext context)
 *             throws Exception {
 *         switch (kind) {
 *             case 1 -> ((AbortChainHandler) handler).handle(context);
 *             case 2 -> ((BlockingHandler) handler).handle(context);
 *             case 3 -> ((ClassRoutingHandler) handler).handle(context);
 *             default -> handler.handle(context);
 *         }
 *     }
 * }
 * }</pre>
 *
 * This is the "switch on a byte id" dispatch strategy for megamorphic call sites that is benchmarked against
 * an {@code instanceof} cascade and plain interface dispatch in
 * <a href="https://shipilev.net/blog/2015/black-magic-method-dispatch/#_cheating_the_runtime_2">this</a> article
 */
public abstract class ServerRestHandlerDispatcher implements HandlerKindResolver {

    public abstract void dispatch(int kind, ServerRestHandler handler, QuarkusResteasyReactiveRequestContext context)
            throws Exception;
}
