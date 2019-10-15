package io.quarkus.restclient.runtime;

import java.util.Set;

import javax.ws.rs.RuntimeType;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.jboss.resteasy.core.providerfactory.ClientHelper;
import org.jboss.resteasy.core.providerfactory.NOOPServerHelper;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.microprofile.client.RestClientBuilderImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public void setSslEnabled(boolean sslEnabled) {
        RestClientBuilderImpl.setSslEnabled(sslEnabled);
    }

    public void initializeResteasyProviderFactory(RuntimeValue<InjectorFactory> injectorFactory, boolean useBuiltIn,
            Set<String> providersToRegister,
            Set<String> contributedProviders) {
        ResteasyProviderFactory clientProviderFactory = new ResteasyProviderFactoryImpl(null, true) {
            @Override
            public RuntimeType getRuntimeType() {
                return RuntimeType.CLIENT;
            }

            @Override
            protected void initializeUtils() {
                clientHelper = new ClientHelper(this);
                serverHelper = NOOPServerHelper.INSTANCE;
            }

            @Override
            public InjectorFactory getInjectorFactory() {
                return injectorFactory.getValue();
            }
        };

        if (useBuiltIn) {
            RegisterBuiltin.register(clientProviderFactory);
            registerProviders(clientProviderFactory, contributedProviders, false);
        } else {
            providersToRegister.removeAll(contributedProviders);
            registerProviders(clientProviderFactory, providersToRegister, true);
            registerProviders(clientProviderFactory, contributedProviders, false);
        }

        RestClientBuilderImpl.setProviderFactory(clientProviderFactory);
    }

    private static void registerProviders(ResteasyProviderFactory clientProviderFactory, Set<String> providersToRegister,
            Boolean isBuiltIn) {
        for (String providerToRegister : providersToRegister) {
            try {
                clientProviderFactory
                        .registerProvider(Thread.currentThread().getContextClassLoader().loadClass(providerToRegister.trim()),
                                isBuiltIn);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find class for provider " + providerToRegister, e);
            }
        }
    }
}
