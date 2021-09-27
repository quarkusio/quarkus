package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;

public class InstanceBean extends BuiltInBean<Instance<?>> {

    public static final Set<Type> INSTANCE_TYPES = Set.of(Instance.class, Object.class);

    static final InstanceBean INSTANCE = new InstanceBean();

    @Override
    public Set<Type> getTypes() {
        return INSTANCE_TYPES;
    }

    @Override
    public Class<?> getBeanClass() {
        return InstanceImpl.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Instance<?> get(CreationalContext<Instance<?>> creationalContext) {
        // Obtain current IP to get the required type and qualifiers
        InjectionPoint ip = InjectionPointProvider.get();
        InstanceImpl<Instance<?>> instance = new InstanceImpl<Instance<?>>((InjectableBean<?>) ip.getBean(), ip.getType(),
                ip.getQualifiers(), (CreationalContextImpl<?>) creationalContext, Collections.EMPTY_SET, ip.getMember(), 0);
        CreationalContextImpl.addDependencyToParent((InjectableBean<Instance<?>>) ip.getBean(), instance, creationalContext);
        return instance;
    }
}
