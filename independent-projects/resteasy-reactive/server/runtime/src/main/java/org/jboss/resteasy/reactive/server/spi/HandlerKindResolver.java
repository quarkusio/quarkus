package org.jboss.resteasy.reactive.server.spi;

/**
 * Assigns a small integer "kind" to each handler of a handler chain when the chain is built.
 * <p>
 * The kinds are opaque to RESTEasy Reactive itself: they are stored next to each handler chain and made available
 * to {@link org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext} so that an implementation can
 * dispatch to the handler of a given position without having to determine its type on every request.
 * The value {@link #UNKNOWN} is used for handlers the resolver does not know about.
 */
public interface HandlerKindResolver {

    byte UNKNOWN = 0;

    byte[] NO_KINDS = new byte[0];

    HandlerKindResolver NONE = new HandlerKindResolver() {
        @Override
        public byte kindOf(ServerRestHandler handler) {
            return UNKNOWN;
        }
    };

    byte kindOf(ServerRestHandler handler);

    default byte[] kindsOf(ServerRestHandler[] chain) {
        if (chain.length == 0) {
            return NO_KINDS;
        }
        byte[] kinds = new byte[chain.length];
        for (int i = 0; i < chain.length; i++) {
            kinds[i] = kindOf(chain[i]);
        }
        return kinds;
    }
}
