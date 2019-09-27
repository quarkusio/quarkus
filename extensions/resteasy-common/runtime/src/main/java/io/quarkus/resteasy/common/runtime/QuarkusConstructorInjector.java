package io.quarkus.resteasy.common.runtime;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.WebApplicationException;

import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

import io.quarkus.arc.runtime.BeanContainer;

public class QuarkusConstructorInjector implements ConstructorInjector {

    private volatile BeanContainer.Factory<?> factory;

    private final ConstructorInjector delegate;

    private final Constructor<?> ctor;

    public QuarkusConstructorInjector(Constructor<?> ctor, ConstructorInjector delegate) {
        this.ctor = ctor;
        this.delegate = delegate;
    }

    @Override
    public CompletionStage<Object> construct(boolean unwrapAsync) {
        if (QuarkusInjectorFactory.CONTAINER == null) {
            return this.delegate.construct(unwrapAsync);
        }
        if (factory == null) {
            factory = QuarkusInjectorFactory.CONTAINER.instanceFactory(this.ctor.getDeclaringClass());
        }
        if (factory == null) {
            return delegate.construct(unwrapAsync);
        }
        return CompletableFuture.completedFuture(factory.create().get());
    }

    @Override
    public CompletionStage<Object> construct(HttpRequest request, HttpResponse response, boolean unwrapAsync)
            throws Failure, WebApplicationException, ApplicationException {
        if (QuarkusInjectorFactory.CONTAINER == null) {
            return delegate.construct(request, response, unwrapAsync);
        }
        if (factory == null) {
            factory = QuarkusInjectorFactory.CONTAINER.instanceFactory(this.ctor.getDeclaringClass());
        }
        if (factory == null) {
            return delegate.construct(request, response, unwrapAsync);
        }
        return CompletableFuture.completedFuture(factory.create().get());
    }

    @Override
    public CompletionStage<Object[]> injectableArguments(boolean unwrapAsync) {
        return this.delegate.injectableArguments(unwrapAsync);
    }

    @Override
    public CompletionStage<Object[]> injectableArguments(HttpRequest request, HttpResponse response, boolean unwrapAsync)
            throws Failure {
        return this.delegate.injectableArguments(request, response, unwrapAsync);
    }
}
