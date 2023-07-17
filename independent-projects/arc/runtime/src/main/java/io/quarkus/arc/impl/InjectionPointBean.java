package io.quarkus.arc.impl;

import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;

public class InjectionPointBean extends BuiltInBean<InjectionPoint> {
    private static final Set<Type> IP_TYPES = Set.of(InjectionPoint.class, Object.class);

    @Override
    public Set<Type> getTypes() {
        return IP_TYPES;
    }

    @Override
    public InjectionPoint get(CreationalContext<InjectionPoint> creationalContext) {
        return InjectionPointProvider.get();
    }

    @Override
    public Class<?> getBeanClass() {
        return InjectionPointImpl.class;
    }
}
