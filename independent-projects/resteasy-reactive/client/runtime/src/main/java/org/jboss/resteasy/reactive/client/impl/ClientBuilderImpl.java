package org.jboss.resteasy.reactive.client.impl;

import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;
import org.jboss.resteasy.reactive.client.spi.ClientContextResolver;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;

public class ClientBuilderImpl extends ClientBuilder {

    private ClientProxies clientProxies;
    private ConfigurationImpl configuration;
    private SSLContext sslContext;
    private KeyStore trustStore;
    private KeyStore keyStore;
    private char[] keystorePassword;
    private HostnameVerifier hostnameVerifier;
    private static final ClientContextResolver CLIENT_CONTEXT_RESOLVER = ClientContextResolver.getInstance();

    @Override
    public ClientBuilder withConfig(Configuration config) {
        this.configuration = new ConfigurationImpl(config);
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        this.keyStore = null;
        this.trustStore = null;
        return this;
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        this.hostnameVerifier = verifier;
        return this;
    }

    @Override
    public ClientBuilder executorService(ExecutorService executorService) {
        return this;
    }

    @Override
    public ClientBuilder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        return this;
    }

    @Override
    public ClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        return this;
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit unit) {
        return this;
    }

    @Override
    public Client build() {
        return new ClientImpl(configuration,
                CLIENT_CONTEXT_RESOLVER.resolve(Thread.currentThread().getContextClassLoader()), hostnameVerifier,
                sslContext);

    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        configuration.property(name, value);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass) {
        configuration.register(componentClass);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass, int priority) {
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass, Class<?>... contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component) {
        configuration.register(component);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component, int priority) {
        configuration.register(component, priority);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component, Class<?>... contracts) {
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public ClientBuilderImpl register(Object component, Map<Class<?>, Integer> contracts) {
        configuration.register(component, contracts);
        return this;
    }
}
