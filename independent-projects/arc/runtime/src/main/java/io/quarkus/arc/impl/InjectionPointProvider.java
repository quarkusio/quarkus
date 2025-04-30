package io.quarkus.arc.impl;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

import io.quarkus.arc.InjectableReferenceProvider;

/**
 *
 * @author Martin Kouba
 */
public class InjectionPointProvider implements InjectableReferenceProvider<InjectionPoint> {

    @Override
    public InjectionPoint get(CreationalContext<InjectionPoint> creationalContext) {
        return getCurrent(creationalContext);
    }

    public static InjectionPoint getCurrent(CreationalContext<?> ctx) {
        return CreationalContextImpl.getCurrentInjectionPoint(ctx);
    }

    /**
     * Set the current injection point for a non-null parameter, or remove it for null parameter.
     *
     * @return the previous injection point or {@code null}
     */
    static InjectionPoint setCurrent(CreationalContext<?> ctx, InjectionPoint ip) {
        // it wouldn't be necessary to reset this, but we do that as a safeguard,
        // to prevent accidental references from keeping these objects alive
        return CreationalContextImpl.setCurrentInjectionPoint(ctx, ip);
    }

}
