package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.ArcInvocationContext;

/**
 * Invocation context for an "inner" invocation chain, consisting of interceptor methods declared
 * in one class and its superclasses. It doesn't proceed to other interceptors in the "outer" invocation
 * chain (interceptor methods declared in other classes).
 */
abstract class InnerInvocationContext implements ArcInvocationContext {

    protected final ArcInvocationContext delegate;
    protected Object[] parameters;

    InnerInvocationContext(InvocationContext delegate, Object[] parameters) {
        this.delegate = (ArcInvocationContext) delegate;
        this.parameters = parameters;
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return delegate.getInterceptorBindings();
    }

    @Override
    public Method getMethod() {
        return delegate.getMethod();
    }

    @Override
    public Object[] getParameters() {
        if (parameters == null) {
            throw new IllegalStateException();
        }
        return parameters;
    }

    @Override
    public void setParameters(Object[] params) {
        if (parameters == null) {
            throw new IllegalStateException();
        }
        AbstractInvocationContext.validateParameters(delegate.getMethod(), params);
        this.parameters = params;
    }

    @Override
    public Object getTarget() {
        return delegate.getTarget();
    }

    @Override
    public Object getTimer() {
        return delegate.getTimer();
    }

    @Override
    public Constructor<?> getConstructor() {
        return delegate.getConstructor();
    }

    @Override
    public Map<String, Object> getContextData() {
        return delegate.getContextData();
    }

    @Override
    public <T extends Annotation> T findIterceptorBinding(Class<T> annotationType) {
        return delegate.findIterceptorBinding(annotationType);
    }

    @Override
    public <T extends Annotation> List<T> findIterceptorBindings(Class<T> annotationType) {
        return delegate.findIterceptorBindings(annotationType);
    }

    @Override
    public Object proceed() throws Exception {
        return proceed(1);
    }

    protected abstract Object proceed(int currentPosition) throws Exception;

    class NextInnerInvocationContext implements ArcInvocationContext {

        private final int position;
        protected Object[] parameters;

        public NextInnerInvocationContext(int position, Object[] parameters) {
            this.position = position;
            this.parameters = parameters;
        }

        @Override
        public Object proceed() throws Exception {
            return InnerInvocationContext.this.proceed(position);
        }

        @Override
        public Object getTarget() {
            return InnerInvocationContext.this.getTarget();
        }

        @Override
        public Object getTimer() {
            return InnerInvocationContext.this.getTimer();
        }

        @Override
        public Method getMethod() {
            return InnerInvocationContext.this.getMethod();
        }

        @Override
        public Constructor<?> getConstructor() {
            return InnerInvocationContext.this.getConstructor();
        }

        @Override
        public Object[] getParameters() {
            return parameters;
        }

        @Override
        public void setParameters(Object[] params) {
            AbstractInvocationContext.validateParameters(InnerInvocationContext.this.delegate.getMethod(), params);
            this.parameters = params;
        }

        @Override
        public Map<String, Object> getContextData() {
            return InnerInvocationContext.this.getContextData();
        }

        @Override
        public Set<Annotation> getInterceptorBindings() {
            return InnerInvocationContext.this.getInterceptorBindings();
        }

        @Override
        public <T extends Annotation> T findIterceptorBinding(Class<T> annotationType) {
            return InnerInvocationContext.this.findIterceptorBinding(annotationType);
        }

        @Override
        public <T extends Annotation> List<T> findIterceptorBindings(Class<T> annotationType) {
            return InnerInvocationContext.this.findIterceptorBindings(annotationType);
        }

    }

}
