package io.quarkus.rest.runtime.model;

import javax.ws.rs.Priorities;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.rest.runtime.spi.BeanFactory;

public class ResourceExceptionMapper<T extends Throwable> {

    private BeanFactory<ExceptionMapper<T>> factory;
    private Integer priority = Priorities.USER;

    public void setFactory(BeanFactory<ExceptionMapper<T>> factory) {
        this.factory = factory;
    }

    public BeanFactory<ExceptionMapper<T>> getFactory() {
        return factory;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
