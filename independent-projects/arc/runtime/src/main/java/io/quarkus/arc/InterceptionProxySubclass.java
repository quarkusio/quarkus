package io.quarkus.arc;

/**
 * Represents an interception proxy. Typically, interception is performed by creating a subclass
 * of the original class and arranging bean instantiation such that the contextual instance
 * is in fact an instance of the subclass, but that isn't always possible. In case of
 * {@link InterceptionProxy}, interception is performed by a proxy that delegates to the actual
 * contextual instance. Such proxy implements this interface.
 */
public interface InterceptionProxySubclass extends Subclass {
    /**
     * @return the contextual instance
     */
    Object arc_delegate();

    /**
     * Attempts to unwrap the object if it represents an interception proxy.
     * <p>
     * This method should only be used with caution. If you unwrap an interception proxy,
     * then certain key functionality will not work as expected.
     *
     * @param <T> the type of the object to unwrap
     * @param obj the object to unwrap
     * @return the contextual instance if the object represents an interception proxy, the object otherwise
     */
    @SuppressWarnings("unchecked")
    static <T> T unwrap(T obj) {
        if (obj instanceof InterceptionProxySubclass proxy) {
            return (T) proxy.arc_delegate();
        }
        return obj;
    }
}
