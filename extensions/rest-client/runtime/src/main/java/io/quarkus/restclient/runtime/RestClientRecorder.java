package io.quarkus.restclient.runtime;

import java.lang.reflect.Field;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.RuntimeType;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.microprofile.client.RestClientBuilderImpl;
import org.jboss.resteasy.microprofile.client.RestClientExtension;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {

    public static ResteasyProviderFactory providerFactory;

    public BeanContainerListener hackAroundExtension() {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                try {
                    Field f = RestClientExtension.class.getDeclaredField("manager");
                    f.setAccessible(true);
                    f.set(null, CDI.current().getBeanManager());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public void initializeResteasyProviderFactory(RuntimeValue<InjectorFactory> ifc, boolean useBuiltIn,
            Set<String> providersToRegister,
            Set<String> contributedProviders) {
        ResteasyProviderFactory clientProviderFactory = new ResteasyProviderFactoryImpl(RuntimeType.CLIENT,
                new ResteasyProviderFactoryImpl()) {
            @Override
            public RuntimeType getRuntimeType() {
                return RuntimeType.CLIENT;
            }

            @Override
            public InjectorFactory getInjectorFactory() {
                return ifc.getValue();
            }
        };

        registerProviders(clientProviderFactory, useBuiltIn, providersToRegister, contributedProviders);
        RestClientBuilderImpl.setProviderFactory(clientProviderFactory);
        ResteasyClientBuilderImpl.setProviderFactory(clientProviderFactory);
        providerFactory = clientProviderFactory;
    }

    private static void registerProviders(ResteasyProviderFactory providerFactory, boolean useBuiltIn,
            Set<String> providersToRegister,
            Set<String> contributedProviders) {
        if (useBuiltIn) {
            RegisterBuiltin.register(providerFactory);
        } else {
            providersToRegister.removeAll(contributedProviders);
            registerProviders(providerFactory, providersToRegister, true);
        }
        registerProviders(providerFactory, contributedProviders, false);
    }

    private static void registerProviders(ResteasyProviderFactory providerFactory, Set<String> providersToRegister,
            Boolean isBuiltIn) {
        for (String providerToRegister : providersToRegister) {
            try {
                providerFactory
                        .registerProvider(Thread.currentThread().getContextClassLoader().loadClass(providerToRegister.trim()),
                                isBuiltIn);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find class for provider " + providerToRegister, e);
            }
        }
    }
}
