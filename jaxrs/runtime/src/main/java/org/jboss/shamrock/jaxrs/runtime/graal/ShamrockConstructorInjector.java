package org.jboss.shamrock.jaxrs.runtime.graal;

import java.lang.reflect.Constructor;

import javax.enterprise.inject.Instance;
import javax.ws.rs.WebApplicationException;

import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

/**
 * Created by bob on 7/31/18.
 */
public class ShamrockConstructorInjector implements ConstructorInjector {
    public ShamrockConstructorInjector(Constructor ctor, ConstructorInjector delegate) {
        this.ctor = ctor;
        this.delegate = delegate;
    }

    @Override
    public Object construct() {
        System.err.println("construct() " + this.ctor);
        return this.delegate.construct();
    }

    @Override
    public Object construct(HttpRequest request, HttpResponse response) throws Failure, WebApplicationException, ApplicationException {
        System.err.println("construct(req,resp) " + this.ctor);
        System.err.println("CAN WE CDI? " + ShamrockInjectorFactory.CONTAINER);
        if (ShamrockInjectorFactory.CONTAINER == null) {
            return delegate.construct(request, response);
        }
        Instance object = ShamrockInjectorFactory.CONTAINER.select(this.ctor.getDeclaringClass());
        return object.get();
    }

    @Override
    public Object[] injectableArguments() {
        return this.delegate.injectableArguments();
    }

    @Override
    public Object[] injectableArguments(HttpRequest request, HttpResponse response) throws Failure {
        return this.delegate.injectableArguments(request, response);
    }

    private final ConstructorInjector delegate;

    private final Constructor ctor;
}
