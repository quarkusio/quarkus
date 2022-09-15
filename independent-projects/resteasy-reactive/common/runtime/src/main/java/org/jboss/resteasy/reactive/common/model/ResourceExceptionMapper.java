package org.jboss.resteasy.reactive.common.model;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceExceptionMapper<T extends Throwable> {

    private BeanFactory<ExceptionMapper<T>> factory;
    private int priority = Priorities.USER;
    private String className;

    public void setFactory(BeanFactory<ExceptionMapper<T>> factory) {
        this.factory = factory;
    }

    public BeanFactory<ExceptionMapper<T>> getFactory() {
        return factory;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getClassName() {
        return className;
    }

    public ResourceExceptionMapper<T> setClassName(String className) {
        this.className = className;
        return this;
    }
}
