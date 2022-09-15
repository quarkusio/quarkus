package org.jboss.resteasy.reactive.common.model;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ParamConverterProvider;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceParamConverterProvider implements Comparable<ResourceParamConverterProvider> {

    private BeanFactory<ParamConverterProvider> factory;
    private Integer priority = Priorities.USER;
    private String className;

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

    public String getClassName() {
        return className;
    }

    public ResourceParamConverterProvider setClassName(String className) {
        this.className = className;
        return this;
    }

    @Override
    public int compareTo(ResourceParamConverterProvider o) {
        return this.priority.compareTo(o.priority);
    }
}
