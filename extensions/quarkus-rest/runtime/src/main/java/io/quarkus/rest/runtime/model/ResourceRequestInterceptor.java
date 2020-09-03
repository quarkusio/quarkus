package io.quarkus.rest.runtime.model;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestFilter;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceRequestInterceptor implements Comparable<ResourceRequestInterceptor> {

    private BeanFactory<ContainerRequestFilter> factory;
    private boolean preMatching;
    private Integer priority = Priorities.USER; // default priority as defined by spec

    public void setFactory(BeanFactory<ContainerRequestFilter> factory) {
        this.factory = factory;
    }

    public BeanFactory<ContainerRequestFilter> getFactory() {
        return factory;
    }

    public void setPreMatching(boolean preMatching) {
        this.preMatching = preMatching;
    }

    public boolean isPreMatching() {
        return preMatching;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    // spec says that request interceptors are sorted in ascending order
    @Override
    public int compareTo(ResourceRequestInterceptor o) {
        return this.priority.compareTo(o.priority);
    }
}
