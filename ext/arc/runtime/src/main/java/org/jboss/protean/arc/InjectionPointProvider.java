package org.jboss.protean.arc;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 *
 * @author Martin Kouba
 */
public class InjectionPointProvider implements InjectableReferenceProvider<InjectionPoint> {

    static final ThreadLocal<InjectionPoint> CURRENT = new ThreadLocal<>();

    @Override
    public InjectionPoint get(CreationalContext<InjectionPoint> creationalContext) {
        return CURRENT.get();
    }

}
