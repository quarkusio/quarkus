package io.quarkus.smallrye.restclient.runtime;

import java.util.Set;

import javax.ws.rs.RuntimeType;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;
import org.jboss.resteasy.core.providerfactory.ClientHelper;
import org.jboss.resteasy.core.providerfactory.NOOPServerHelper;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SmallRyeRestClientRecorder {
    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

    public void setSslEnabled(boolean sslEnabled) {
        RestClientBuilderImpl.SSL_ENABLED = sslEnabled;
    }

    public void initializeResteasyProviderFactory(boolean useBuiltIn, Set<String> providersToRegister,
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
        };

        if (useBuiltIn) {
            RegisterBuiltin.register(clientProviderFactory);
            registerProviders(clientProviderFactory, contributedProviders);
        } else {
            registerProviders(clientProviderFactory, providersToRegister);
        }

        RestClientBuilderImpl.PROVIDER_FACTORY = clientProviderFactory;
    }

    private static void registerProviders(ResteasyProviderFactory clientProviderFactory, Set<String> providersToRegister) {
        for (String providerToRegister : providersToRegister) {
            try {
                clientProviderFactory
                        .registerProvider(Thread.currentThread().getContextClassLoader().loadClass(providerToRegister.trim()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to find class for provider " + providerToRegister, e);
            }
        }
    }
}
