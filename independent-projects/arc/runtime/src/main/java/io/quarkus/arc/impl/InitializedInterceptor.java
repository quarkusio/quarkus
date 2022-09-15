package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableInterceptor;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class InitializedInterceptor<T> implements InjectableInterceptor<T> {

    public static <I> InitializedInterceptor<I> of(I interceptorInstance, InjectableInterceptor<I> delegate) {
        return new InitializedInterceptor<>(interceptorInstance, delegate);
    }

    private final T interceptorInstance;

    private final InjectableInterceptor<T> delegate;

    InitializedInterceptor(T interceptorInstance, InjectableInterceptor<T> delegate) {
        this.interceptorInstance = interceptorInstance;
        this.delegate = delegate;
    }

    @Override
    public String getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return delegate.getScope();
    }

    @Override
    public Set<Type> getTypes() {
        return delegate.getTypes();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return delegate.getQualifiers();
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        return delegate.create(creationalContext);
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        delegate.destroy(instance, creationalContext);
    }

    @Override
    public T get(CreationalContext<T> creationalContext) {
        return interceptorInstance;
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return delegate.getInterceptorBindings();
    }

    @Override
    public boolean intercepts(InterceptionType type) {
        return delegate.intercepts(type);
    }

    @Override
    public Object intercept(InterceptionType type, T instance, InvocationContext ctx) throws Exception {
        return delegate.intercept(type, instance, ctx);
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }

    @Override
    public Class<?> getBeanClass() {
        return delegate.getBeanClass();
    }

}
