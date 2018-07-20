package org.jboss.shamrock.deployment;

import java.util.Set;

public interface InjectionProvider {

    Set<Class<?>> getProvidedTypes();

    <T> T getInstance(Class<T> type);

}
