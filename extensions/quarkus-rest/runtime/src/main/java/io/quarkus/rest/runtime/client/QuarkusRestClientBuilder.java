package io.quarkus.rest.runtime.client;

import java.security.KeyStore;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Configuration;

import io.quarkus.rest.runtime.QuarkusRestRecorder;
import io.quarkus.rest.runtime.core.QuarkusRestDeployment;
import io.quarkus.rest.runtime.core.Serialisers;

public class QuarkusRestClientBuilder extends ClientBuilder {

    private ClientProxies clientProxies;
    private Configuration configuration;
    private SSLContext sslContext;
    private KeyStore trustStore;
    private KeyStore keyStore;
    private char[] keystorePassword;
    private HostnameVerifier hostnameVerifier;

    @Override
    public ClientBuilder withConfig(Configuration config) {
        this.configuration = config;
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
        QuarkusRestDeployment currentDeployment = QuarkusRestRecorder.getCurrentDeployment();
        if (currentDeployment == null) {
            return new QuarkusRestClient(new Serialisers(),
                    new ClientProxies(Collections.emptyMap()), hostnameVerifier, sslContext);
        } else {
            return new QuarkusRestClient(currentDeployment.getSerialisers(),
                    currentDeployment.getClientProxies(), hostnameVerifier, sslContext);
        }
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public ClientBuilder property(String name, Object value) {
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass) {
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, int priority) {
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        return this;
    }

    @Override
    public ClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        return this;
    }

    @Override
    public ClientBuilder register(Object component) {
        return this;
    }

    @Override
    public ClientBuilder register(Object component, int priority) {
        return this;
    }

    @Override
    public ClientBuilder register(Object component, Class<?>... contracts) {
        return null;
    }

    @Override
    public ClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        return this;
    }
}
