package org.jboss.shamrock.weld.deployment;

import java.util.Collections;
import java.util.Set;

import org.jboss.shamrock.deployment.InjectionProvider;

public class WeldInjectionProvider implements InjectionProvider {

    private final WeldDeployment deployment = new WeldDeployment();

    @Override
    public Set<Class<?>> getProvidedTypes() {
        return Collections.singleton(WeldDeployment.class);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        if(type == WeldDeployment.class) {
            return (T) deployment;
        }
        throw new IllegalArgumentException("invalid type");
    }
}
