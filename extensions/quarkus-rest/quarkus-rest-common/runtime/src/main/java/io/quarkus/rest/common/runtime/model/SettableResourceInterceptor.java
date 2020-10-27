package io.quarkus.rest.common.runtime.model;

import java.util.Set;

import io.quarkus.rest.spi.BeanFactory;

public interface SettableResourceInterceptor<T> {

    void setFactory(BeanFactory<T> factory);

    void setNameBindingNames(Set<String> nameBindings);

    void setPriority(Integer priority);
}
