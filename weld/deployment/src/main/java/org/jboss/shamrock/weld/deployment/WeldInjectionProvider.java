package org.jboss.shamrock.weld.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.shamrock.deployment.InjectionProvider;

public class WeldInjectionProvider implements InjectionProvider {

    private final WeldDeployment deployment = new WeldDeployment();
    private final BeanArchiveIndex beanArchiveIndex = new BeanArchiveIndex();

    @Override
    public Set<Class<?>> getProvidedTypes() {

        return new HashSet<>(Arrays.asList(WeldDeployment.class, BeanArchiveIndex.class));
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        if (type == WeldDeployment.class) {
            return (T) deployment;
        }
        if (type == BeanArchiveIndex.class) {
            return (T) beanArchiveIndex;
        }
        throw new IllegalArgumentException("invalid type");
    }
}
