package io.quarkus.arc;

/**
 * Represents a client proxy - a contextual reference to a bean with a normal scope.
 *
 * <p>
 *
 * Client proxy delegates all method invocations to a target contextual instance. Note that only method invocations are
 * delegated. So a field of a normal scoped bean should never be read or written in order to avoid working with non-contextual
 * or stale data.
 *
 * <p>
 *
 * In general, client proxies allow for:
 * <ul>
 * <li>Lazy instantiation - the instance is created once a method is invoked upon the proxy.</li>
 * <li>Ability to inject a bean with "narrower" scope to a bean with "wider" scope; i.e. you can inject a {@code @RequestScoped}
 * bean into an {@code @ApplicationScoped} bean.</li>
 * <li>Circular dependencies in the dependency graph. Having circular dependencies is often an indication that a redesign should
 * be considered, but sometimes it’s inevitable.</li>
 * <li>In rare cases it’s practical to destroy the beans manually. A direct injected reference would lead to a stale bean
 * instance.</li>
 * </ul>
 */
public interface ClientProxy {

    /**
     *
     * @return the contextual instance
     */
    Object arc_contextualInstance();

    /**
     *
     * @return the bean
     */
    InjectableBean<?> arc_bean();

    /**
     * Attempts to unwrap the object if it represents a client proxy.
     * <p>
     * This method should only be used with caution. If you unwrap a client proxy then certain key functionality will not work
     * as expected.
     *
     * @param <T> the type of the object to unwrap
     * @param obj the object to unwrap
     * @return the contextual instance if the object represents a client proxy, the object otherwise
     */
    @SuppressWarnings("unchecked")
    static <T> T unwrap(T obj) {
        if (obj instanceof ClientProxy proxy) {
            return (T) proxy.arc_contextualInstance();
        }
        return obj;
    }

}
