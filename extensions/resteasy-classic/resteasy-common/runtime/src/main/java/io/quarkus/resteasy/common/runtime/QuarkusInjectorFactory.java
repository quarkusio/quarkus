package io.quarkus.resteasy.common.runtime;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.ws.rs.WebApplicationException;

import org.jboss.logging.Logger;
import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;

import io.quarkus.arc.runtime.ClientProxyUnwrapper;

public class QuarkusInjectorFactory extends InjectorFactoryImpl {

    private static final Logger log = Logger.getLogger("io.quarkus.resteasy.runtime");
    static final Function<Object, Object> PROXY_UNWRAPPER = new ClientProxyUnwrapper();

    @SuppressWarnings("rawtypes")
    @Override
    public ConstructorInjector createConstructor(Constructor constructor, ResteasyProviderFactory providerFactory) {
        if (constructor == null) {
            throw new IllegalStateException(
                    "Unable to locate proper constructor for dynamically registered provider. Make sure the class has a no-args constructor and that it uses '@Context' for field injection if necessary.");
        }
        log.debugf("Create constructor: %s", constructor);
        return new QuarkusConstructorInjector(constructor, super.createConstructor(constructor, providerFactory));
    }

    @Override
    public ConstructorInjector createConstructor(ResourceConstructor constructor, ResteasyProviderFactory providerFactory) {
        log.debugf("Create resource constructor: %s", constructor.getConstructor());
        return new QuarkusConstructorInjector(constructor.getConstructor(),
                super.createConstructor(constructor, providerFactory));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PropertyInjector createPropertyInjector(Class resourceClass, ResteasyProviderFactory providerFactory) {
        PropertyInjector delegate = super.createPropertyInjector(resourceClass, providerFactory);
        return new UnwrappingPropertyInjector(delegate);
    }

    @Override
    public PropertyInjector createPropertyInjector(ResourceClass resourceClass, ResteasyProviderFactory providerFactory) {
        PropertyInjector delegate = super.createPropertyInjector(resourceClass, providerFactory);
        return new UnwrappingPropertyInjector(delegate);
    }

    private static class UnwrappingPropertyInjector implements PropertyInjector {
        private final PropertyInjector delegate;

        public UnwrappingPropertyInjector(PropertyInjector delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletionStage<Void> inject(Object target, boolean unwrapAsync) {
            return delegate.inject(PROXY_UNWRAPPER.apply(target), unwrapAsync);
        }

        @Override
        public CompletionStage<Void> inject(HttpRequest request, HttpResponse response, Object target, boolean unwrapAsync)
                throws Failure, WebApplicationException, ApplicationException {
            return delegate.inject(request, response, PROXY_UNWRAPPER.apply(target), unwrapAsync);
        }
    }
}
