package io.quarkus.resteasy.common.runtime;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import javax.ws.rs.WebApplicationException;

import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public class QuarkusConstructorInjector implements ConstructorInjector {

    private volatile Supplier<? extends InstanceHandle<?>> factory;

    private final ConstructorInjector delegate;

    private final Constructor<?> ctor;

    public QuarkusConstructorInjector(Constructor<?> ctor, ConstructorInjector delegate) {
        this.ctor = ctor;
        this.delegate = delegate;
    }

    @Override
    public Object construct(boolean unwrapAsync) {
        if (factory != null) {
            return factory.get().get();
        }
        factory = Arc.container().instanceSupplier(this.ctor.getDeclaringClass());
        if (factory == null) {
            return delegate.construct(unwrapAsync);
        }
        return factory.get().get();
    }

    @Override
    public Object construct(HttpRequest request, HttpResponse response, boolean unwrapAsync)
            throws Failure, WebApplicationException, ApplicationException {
        if (factory != null) {
            return factory.get().get();
        }
        factory = Arc.container().instanceSupplier(this.ctor.getDeclaringClass());
        if (factory == null) {
            return delegate.construct(request, response, unwrapAsync);
        }
        return factory.get().get();
    }

    @Override
    public Object injectableArguments(boolean unwrapAsync) {
        return this.delegate.injectableArguments(unwrapAsync);
    }

    @Override
    public Object injectableArguments(HttpRequest request, HttpResponse response, boolean unwrapAsync)
            throws Failure {
        return this.delegate.injectableArguments(request, response, unwrapAsync);
    }
}
