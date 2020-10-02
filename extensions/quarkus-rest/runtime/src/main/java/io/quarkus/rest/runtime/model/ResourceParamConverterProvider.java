package io.quarkus.rest.runtime.model;

import javax.ws.rs.Priorities;
import javax.ws.rs.ext.ParamConverterProvider;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceParamConverterProvider implements Comparable<ResourceParamConverterProvider> {

    private BeanFactory<ParamConverterProvider> factory;
    private Integer priority = Priorities.USER;

    public void setFactory(BeanFactory<ParamConverterProvider> factory) {
        this.factory = factory;
    }

    public BeanFactory<ParamConverterProvider> getFactory() {
        return factory;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(ResourceParamConverterProvider o) {
        return this.priority.compareTo(o.priority);
    }
}
