package org.jboss.resteasy.reactive.common.model;

import java.util.function.Supplier;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ResourceExceptionMapper<T extends Throwable> implements Comparable<ResourceExceptionMapper<?>> {

    private BeanFactory<ExceptionMapper<T>> factory;
    private int priority = Priorities.USER;
    private String className;

    private Supplier<Boolean> discardAtRuntime;

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

    public Supplier<Boolean> getDiscardAtRuntime() {
        return discardAtRuntime;
    }

    public void setDiscardAtRuntime(Supplier<Boolean> discardAtRuntime) {
        this.discardAtRuntime = discardAtRuntime;
    }

    @Override
    public int compareTo(ResourceExceptionMapper<?> o) {
        return Integer.compare(this.priority, o.priority);
    }

    public static final class DiscardAtRuntimeIfBeanIsUnavailable implements Supplier<Boolean> {
        private final String beanClass;

        public DiscardAtRuntimeIfBeanIsUnavailable(String beanClass) {
            this.beanClass = beanClass;
        }

        @Override
        public Boolean get() {
            throw new IllegalStateException("should never be called");
        }

        public String getBeanClass() {
            return beanClass;
        }
    }
}
