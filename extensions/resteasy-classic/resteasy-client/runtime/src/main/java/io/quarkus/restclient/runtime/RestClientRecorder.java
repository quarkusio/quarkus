package io.quarkus.restclient.runtime;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.RuntimeType;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.microprofile.client.RestClientBuilderImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class RestClientRecorder {

    public static ResteasyProviderFactory providerFactory;

    public static void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public static void initializeResteasyProviderFactory(InjectorFactory ifc, boolean useBuiltIn,
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
                return ifc;
            }
        };

        registerProviders(clientProviderFactory, useBuiltIn, providersToRegister, contributedProviders);
        RestClientBuilderImpl.setProviderFactory(clientProviderFactory);
        ResteasyClientBuilderImpl.setProviderFactory(clientProviderFactory);
        providerFactory = clientProviderFactory;
    }

    public static void setResteasyProviderFactoryInstance() {
        ResteasyProviderFactory.setInstance(providerFactory);
    }

    private static void registerProviders(ResteasyProviderFactory providerFactory, boolean useBuiltIn,
            Set<String> providersToRegister,
            Set<String> contributedProviders) {
        if (useBuiltIn) {
            RegisterBuiltin.register(providerFactory);
        } else {
            Set<String> providers = new HashSet<>(providersToRegister);
            providers.removeAll(contributedProviders);
            registerProviders(providerFactory, providers, true);
        }
        registerProviders(providerFactory, contributedProviders, false);
    }

    private static void registerProviders(ResteasyProviderFactory providerFactory, Set<String> providersToRegister,
            Boolean isBuiltIn) {
        for (String providerToRegister : providersToRegister) {
            try {
                providerFactory
                        .registerProvider(
                                Thread.currentThread().getContextClassLoader().loadClass(providerToRegister.trim()),
                                isBuiltIn);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find class for provider " + providerToRegister, e);
            }
        }
    }
}
