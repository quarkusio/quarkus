package org.jboss.shamrock.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BeanDeploymentInjectionProvider implements InjectionProvider {

    private final BeanDeployment deployment = new BeanDeployment();

    private final BeanArchiveIndex beanArchiveIndex = new BeanArchiveIndex();

    @Override
    public Set<Class<?>> getProvidedTypes() {
        return new HashSet<>(Arrays.asList(BeanDeployment.class, BeanArchiveIndex.class));
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        if(type == BeanDeployment.class) {
            return (T) deployment;
        }
        if (type == BeanArchiveIndex.class) {
            return (T) beanArchiveIndex;
        }
        throw new IllegalArgumentException("invalid type");
    }
}
