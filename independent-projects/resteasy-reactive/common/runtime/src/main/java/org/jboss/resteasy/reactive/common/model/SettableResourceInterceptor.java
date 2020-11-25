package org.jboss.resteasy.reactive.common.model;

import java.util.Set;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public interface SettableResourceInterceptor<T> {

    void setFactory(BeanFactory<T> factory);

    void setNameBindingNames(Set<String> nameBindings);

    void setPriority(Integer priority);
}
