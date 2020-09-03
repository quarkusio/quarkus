package io.quarkus.rest.runtime.model;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerResponseFilter;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceResponseInterceptor implements Comparable<ResourceResponseInterceptor> {

    private BeanFactory<ContainerResponseFilter> factory;
    private Integer priority = Priorities.USER; // default priority as defined by spec

    public void setFactory(BeanFactory<ContainerResponseFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerResponseFilter> getFactory() {
        return factory;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    // spec says that request interceptors are sorted in descending order
    @Override
    public int compareTo(ResourceResponseInterceptor o) {
        return o.priority.compareTo(this.priority);
    }
}
