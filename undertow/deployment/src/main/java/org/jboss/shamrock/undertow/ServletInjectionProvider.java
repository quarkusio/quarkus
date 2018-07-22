package org.jboss.shamrock.undertow;

import java.util.Collections;
import java.util.Set;

import org.jboss.shamrock.deployment.InjectionProvider;

public class ServletInjectionProvider implements InjectionProvider {

    private final ServletDeployment deployment = new ServletDeployment();

    @Override
    public Set<Class<?>> getProvidedTypes() {
        return Collections.singleton(ServletDeployment.class);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return (T) deployment;
    }
}
