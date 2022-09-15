package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableReferenceProvider;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 *
 * @author Martin Kouba
 */
public class InjectionPointProvider implements InjectableReferenceProvider<InjectionPoint> {

    private static final ThreadLocal<InjectionPoint> CURRENT = new ThreadLocal<>();

    @Override
    public InjectionPoint get(CreationalContext<InjectionPoint> creationalContext) {
        return CURRENT.get();
    }

    /**
     * Set the current injection point for a non-null parameter, remove the threadlocal for null parameter.
     *
     * @param injectionPoint
     * @return the previous injection point or {@code null}
     */
    static InjectionPoint set(InjectionPoint injectionPoint) {
        if (injectionPoint != null) {
            InjectionPoint prev = InjectionPointProvider.CURRENT.get();
            if (injectionPoint.equals(prev)) {
                return injectionPoint;
            } else {
                InjectionPointProvider.CURRENT.set(injectionPoint);
                return prev;
            }
        } else {
            CURRENT.remove();
            return null;
        }
    }

    public static InjectionPoint get() {
        return CURRENT.get();
    }

}
