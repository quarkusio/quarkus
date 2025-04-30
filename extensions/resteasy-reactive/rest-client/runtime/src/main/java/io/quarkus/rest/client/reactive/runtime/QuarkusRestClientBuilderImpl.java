package io.quarkus.rest.client.reactive.runtime;

import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.core.Configuration;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.rest.client.reactive.runtime.context.ClientHeadersFactoryContextResolver;
import io.quarkus.rest.client.reactive.runtime.context.HttpClientOptionsContextResolver;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.http.HttpClientOptions;

public class QuarkusRestClientBuilderImpl implements QuarkusRestClientBuilder {

    private final RestClientBuilderImpl delegate;

    public QuarkusRestClientBuilderImpl(RestClientBuilderImpl delegate) {
        this.delegate = delegate;
    }

    @Override
    public QuarkusRestClientBuilder baseUrl(URL url) {
        delegate.baseUrl(url);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder baseUri(URI uri) {
        delegate.baseUri(uri);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        delegate.connectTimeout(timeout, unit);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder readTimeout(long timeout, TimeUnit unit) {
        delegate.readTimeout(timeout, unit);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder tlsConfiguration(TlsConfiguration tlsConfiguration) {
        delegate.tlsConfiguration(tlsConfiguration);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder sslContext(SSLContext sslContext) {
        delegate.sslContext(sslContext);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder verifyHost(boolean verifyHost) {
        delegate.verifyHost(verifyHost);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder trustStore(KeyStore trustStore) {
        delegate.trustStore(trustStore);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder trustStore(KeyStore trustStore, String trustStorePassword) {
        delegate.trustStore(trustStore, trustStorePassword);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder keyStore(KeyStore keyStore, String keystorePassword) {
        delegate.keyStore(keyStore, keystorePassword);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder hostnameVerifier(HostnameVerifier hostnameVerifier) {
        delegate.hostnameVerifier(hostnameVerifier);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder followRedirects(boolean follow) {
        delegate.followRedirects(follow);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder proxyAddress(String proxyHost, int proxyPort) {
        delegate.proxyAddress(proxyHost, proxyPort);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder proxyPassword(String proxyPassword) {
        delegate.proxyPassword(proxyPassword);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder proxyUser(String proxyUser) {
        delegate.proxyUser(proxyUser);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder nonProxyHosts(String nonProxyHosts) {
        delegate.nonProxyHosts(nonProxyHosts);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder multipartPostEncoderMode(String mode) {
        delegate.multipartPostEncoderMode(mode);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder queryParamStyle(QueryParamStyle style) {
        delegate.queryParamStyle(style);
        return this;
    }

    @Override
    public Configuration getConfiguration() {
        return delegate.getConfiguration();
    }

    @Override
    public QuarkusRestClientBuilder property(String name, Object value) {
        delegate.property(name, value);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass) {
        delegate.register(componentClass);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass, int priority) {
        delegate.register(componentClass, priority);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        delegate.register(componentClass, contracts);
        return null;
    }

    @Override
    public QuarkusRestClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        delegate.register(componentClass, contracts);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component) {
        delegate.register(component);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component, int priority) {
        delegate.register(component, priority);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component, Class<?>... contracts) {
        delegate.register(component, contracts);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        delegate.register(component, contracts);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder clientHeadersFactory(Class<? extends ClientHeadersFactory> clientHeadersFactoryClass) {
        ClientHeadersFactory bean = BeanGrabber.getBeanIfDefined(clientHeadersFactoryClass);
        if (bean == null) {
            throw new IllegalArgumentException("Failed to instantiate the client headers factory " + clientHeadersFactoryClass
                    + ". Make sure the bean is properly configured for CDI injection.");
        }

        return clientHeadersFactory(bean);
    }

    @Override
    public QuarkusRestClientBuilder clientHeadersFactory(ClientHeadersFactory clientHeadersFactory) {
        delegate.register(new ClientHeadersFactoryContextResolver(clientHeadersFactory));
        return this;
    }

    @Override
    public QuarkusRestClientBuilder httpClientOptions(Class<? extends HttpClientOptions> httpClientOptionsClass) {
        HttpClientOptions bean = BeanGrabber.getBeanIfDefined(httpClientOptionsClass);
        if (bean == null) {
            throw new IllegalArgumentException("Failed to instantiate the HTTP client options " + httpClientOptionsClass
                    + ". Make sure the bean is properly configured for CDI injection.");
        }

        return httpClientOptions(bean);
    }

    @Override
    public QuarkusRestClientBuilder httpClientOptions(HttpClientOptions httpClientOptions) {
        delegate.register(new HttpClientOptionsContextResolver(httpClientOptions));
        return this;
    }

    @Override
    public QuarkusRestClientBuilder clientLogger(ClientLogger clientLogger) {
        delegate.clientLogger(clientLogger);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder loggingScope(LoggingScope loggingScope) {
        delegate.loggingScope(loggingScope);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder loggingBodyLimit(Integer limit) {
        delegate.loggingBodyLimit(limit);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder trustAll(boolean trustAll) {
        delegate.trustAll(trustAll);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder userAgent(String userAgent) {
        delegate.userAgent(userAgent);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder disableDefaultMapper(Boolean disable) {
        delegate.disableDefaultMapper(disable);
        return this;
    }

    @Override
    public QuarkusRestClientBuilder enableCompression(boolean enableCompression) {
        delegate.enableCompression(enableCompression);
        return this;
    }

    @Override
    public <T> T build(Class<T> clazz) throws IllegalStateException, RestClientDefinitionException {
        return delegate.build(clazz);
    }
}
