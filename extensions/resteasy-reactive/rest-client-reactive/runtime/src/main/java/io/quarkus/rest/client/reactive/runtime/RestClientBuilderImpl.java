package io.quarkus.rest.client.reactive.runtime;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.api.InvalidRestClientDefinitionException;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.jaxrs.MultiQueryParamMode;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.rest.client.reactive.runtime.ProxyAddressUtil.HostAndPort;
import io.quarkus.restclient.config.RestClientLoggingConfig;
import io.quarkus.restclient.config.RestClientsConfig;

/**
 * Builder implementation for MicroProfile Rest Client
 */
public class RestClientBuilderImpl implements RestClientBuilder {

    private static final String DEFAULT_MAPPER_DISABLED = "microprofile.rest.client.disable.default.mapper";
    private static final String TLS_TRUST_ALL = "quarkus.tls.trust-all";

    private final ClientBuilderImpl clientBuilder = (ClientBuilderImpl) new ClientBuilderImpl()
            .withConfig(new ConfigurationImpl(RuntimeType.CLIENT));
    private final List<ResponseExceptionMapper<?>> exceptionMappers = new ArrayList<>();
    private final List<ParamConverterProvider> paramConverterProviders = new ArrayList<>();

    private URI uri;
    private boolean followRedirects;
    private QueryParamStyle queryParamStyle;

    private String proxyHost;
    private Integer proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private String nonProxyHosts;

    @Override
    public RestClientBuilderImpl baseUrl(URL url) {
        try {
            this.uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to convert REST client URL to URI", e);
        }
        return this;
    }

    @Override
    public RestClientBuilderImpl connectTimeout(long timeout, TimeUnit timeUnit) {
        clientBuilder.connectTimeout(timeout, timeUnit);
        return this;
    }

    @Override
    public RestClientBuilderImpl readTimeout(long timeout, TimeUnit timeUnit) {
        clientBuilder.readTimeout(timeout, timeUnit);
        return this;
    }

    @Override
    public RestClientBuilderImpl sslContext(SSLContext sslContext) {
        clientBuilder.sslContext(sslContext);
        return this;
    }

    public RestClientBuilderImpl verifyHost(boolean verifyHost) {
        clientBuilder.verifyHost(verifyHost);
        return this;
    }

    @Override
    public RestClientBuilderImpl trustStore(KeyStore trustStore) {
        clientBuilder.trustStore(trustStore);
        return this;
    }

    public RestClientBuilderImpl trustStore(KeyStore trustStore, String trustStorePassword) {
        clientBuilder.trustStore(trustStore, trustStorePassword.toCharArray());
        return this;
    }

    @Override
    public RestClientBuilderImpl keyStore(KeyStore keyStore, String keystorePassword) {
        clientBuilder.keyStore(keyStore, keystorePassword);
        return this;
    }

    @Override
    public RestClientBuilderImpl hostnameVerifier(HostnameVerifier hostnameVerifier) {
        clientBuilder.hostnameVerifier(hostnameVerifier);
        return this;
    }

    @Override
    public RestClientBuilderImpl followRedirects(final boolean follow) {
        this.followRedirects = follow;
        return this;
    }

    @Override
    public RestClientBuilderImpl proxyAddress(final String proxyHost, final int proxyPort) {
        if (proxyHost == null) {
            throw new IllegalArgumentException("proxyHost must not be null");
        }
        if ((proxyPort <= 0 || proxyPort > 65535) && !proxyHost.equals("none")) {
            throw new IllegalArgumentException("Invalid port number");
        }
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;

        return this;
    }

    public RestClientBuilderImpl proxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public RestClientBuilderImpl proxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        return this;
    }

    public RestClientBuilderImpl nonProxyHosts(String nonProxyHosts) {
        this.nonProxyHosts = nonProxyHosts;
        return this;
    }

    @Override
    public RestClientBuilderImpl executorService(ExecutorService executor) {
        throw new IllegalArgumentException("Specifying executor service is not supported. " +
                "The underlying call in RestEasy Reactive is non-blocking, " +
                "there is no reason to offload the call to a separate thread pool.");
    }

    @Override
    public Configuration getConfiguration() {
        return clientBuilder.getConfiguration();
    }

    @Override
    public RestClientBuilderImpl property(String name, Object value) {
        clientBuilder.property(name, value);
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Class<?> componentClass) {
        Object bean = BeanGrabber.getBeanIfDefined(componentClass);
        if (bean != null) {
            registerMpSpecificProvider(bean);
            clientBuilder.register(bean);
        } else {
            registerMpSpecificProvider(componentClass);
            clientBuilder.register(componentClass);
        }
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Class<?> componentClass, int priority) {
        InstanceHandle<?> instance = Arc.container().instance(componentClass);
        if (instance.isAvailable()) {
            registerMpSpecificProvider(instance.get());
            clientBuilder.register(instance.get(), priority);
        } else {
            registerMpSpecificProvider(componentClass);
            clientBuilder.register(componentClass, priority);
        }
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Class<?> componentClass, Class<?>... contracts) {
        InstanceHandle<?> instance = Arc.container().instance(componentClass);
        if (instance.isAvailable()) {
            registerMpSpecificProvider(instance.get());
            clientBuilder.register(instance.get(), contracts);
        } else {
            registerMpSpecificProvider(componentClass);
            clientBuilder.register(componentClass, contracts);
        }
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        InstanceHandle<?> instance = Arc.container().instance(componentClass);
        if (instance.isAvailable()) {
            registerMpSpecificProvider(instance.get());
            clientBuilder.register(instance.get(), contracts);
        } else {
            registerMpSpecificProvider(componentClass);
            clientBuilder.register(componentClass, contracts);
        }
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Object component) {
        registerMpSpecificProvider(component);
        clientBuilder.register(component);
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Object component, int priority) {
        registerMpSpecificProvider(component);
        clientBuilder.register(component, priority);
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Object component, Class<?>... contracts) {
        registerMpSpecificProvider(component);
        clientBuilder.register(component, contracts);
        return this;
    }

    @Override
    public RestClientBuilderImpl register(Object component, Map<Class<?>, Integer> contracts) {
        registerMpSpecificProvider(component);
        clientBuilder.register(component, contracts);
        return this;
    }

    @Override
    public RestClientBuilderImpl baseUri(URI uri) {
        this.uri = uri;
        return this;
    }

    private void registerMpSpecificProvider(Class<?> componentClass) {
        if (ResponseExceptionMapper.class.isAssignableFrom(componentClass)
                || ParamConverterProvider.class.isAssignableFrom(componentClass)) {
            try {
                registerMpSpecificProvider(componentClass.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalArgumentException("Failed to instantiate provider " + componentClass
                        + ". Does it have a public no-arg constructor?", e);
            }
        }
    }

    private void registerMpSpecificProvider(Object component) {
        if (component instanceof ResponseExceptionMapper) {
            exceptionMappers.add((ResponseExceptionMapper<?>) component);
        }
        if (component instanceof ParamConverterProvider) {
            paramConverterProviders.add((ParamConverterProvider) component);
        }
    }

    @Override
    public RestClientBuilderImpl queryParamStyle(final QueryParamStyle style) {
        queryParamStyle = style;
        return this;
    }

    @Override
    public <T> T build(Class<T> aClass) throws IllegalStateException, RestClientDefinitionException {
        if (uri == null) {
            // mandated by the spec
            throw new IllegalStateException("No URL specified. Cannot build a rest client without URL");
        }

        RestClientListeners.get().forEach(listener -> listener.onNewClient(aClass, this));

        AnnotationRegisteredProviders annotationRegisteredProviders = Arc.container()
                .instance(AnnotationRegisteredProviders.class).get();
        for (Map.Entry<Class<?>, Integer> mapper : annotationRegisteredProviders.getProviders(aClass).entrySet()) {
            register(mapper.getKey(), mapper.getValue());
        }

        Object defaultMapperDisabled = getConfiguration().getProperty(DEFAULT_MAPPER_DISABLED);
        Boolean globallyDisabledMapper = ConfigProvider.getConfig()
                .getOptionalValue(DEFAULT_MAPPER_DISABLED, Boolean.class).orElse(false);
        if (!globallyDisabledMapper && !(defaultMapperDisabled instanceof Boolean && (Boolean) defaultMapperDisabled)) {
            exceptionMappers.add(new DefaultMicroprofileRestClientExceptionMapper());
        }

        exceptionMappers.sort(Comparator.comparingInt(ResponseExceptionMapper::getPriority));
        clientBuilder.register(new MicroProfileRestClientResponseFilter(exceptionMappers));
        clientBuilder.followRedirects(followRedirects);

        RestClientsConfig restClientsConfig = Arc.container().instance(RestClientsConfig.class).get();

        RestClientLoggingConfig logging = restClientsConfig.logging;
        LoggingScope loggingScope = logging != null ? logging.scope.map(LoggingScope::forName).orElse(LoggingScope.NONE)
                : LoggingScope.NONE;
        Integer loggingBodySize = logging != null ? logging.bodyLimit : 100;
        clientBuilder.loggingScope(loggingScope);
        clientBuilder.loggingBodySize(loggingBodySize);

        clientBuilder.multiQueryParamMode(toMultiQueryParamMode(queryParamStyle));

        Boolean trustAll = ConfigProvider.getConfig().getOptionalValue(TLS_TRUST_ALL, Boolean.class)
                .orElse(false);

        clientBuilder.trustAll(trustAll);
        restClientsConfig.verifyHost.ifPresent(clientBuilder::verifyHost);

        String userAgent = (String) getConfiguration().getProperty(QuarkusRestClientProperties.USER_AGENT);
        if (userAgent != null) {
            clientBuilder.setUserAgent(userAgent);
        } else if (restClientsConfig.userAgent.isPresent()) {
            clientBuilder.setUserAgent(restClientsConfig.userAgent.get());
        }

        if (proxyHost != null) {
            configureProxy(proxyHost, proxyPort, proxyUser, proxyPassword, nonProxyHosts);
        } else if (restClientsConfig.proxyAddress.isPresent()) {
            HostAndPort globalProxy = ProxyAddressUtil.parseAddress(restClientsConfig.proxyAddress.get());
            configureProxy(globalProxy.host, globalProxy.port, restClientsConfig.proxyUser.orElse(null),
                    restClientsConfig.proxyPassword.orElse(null), restClientsConfig.nonProxyHosts.orElse(null));
        }
        ClientImpl client = clientBuilder.build();
        WebTargetImpl target = (WebTargetImpl) client.target(uri);
        target.setParamConverterProviders(paramConverterProviders);
        try {
            return target.proxy(aClass);
        } catch (InvalidRestClientDefinitionException e) {
            throw new RestClientDefinitionException(e);
        }
    }

    private void configureProxy(String proxyHost, Integer proxyPort, String proxyUser, String proxyPassword,
            String nonProxyHosts) {
        if (proxyHost != null) {
            clientBuilder.proxy(proxyHost, proxyPort);
            if (proxyUser != null && proxyPassword != null) {
                clientBuilder.proxyUser(proxyUser);
                clientBuilder.proxyPassword(proxyPassword);
            }

            if (nonProxyHosts != null) {
                clientBuilder.nonProxyHosts(nonProxyHosts);
            }
        }
    }

    private MultiQueryParamMode toMultiQueryParamMode(QueryParamStyle queryParamStyle) {
        if (queryParamStyle == null) {
            return null;
        }
        switch (queryParamStyle) {
            case MULTI_PAIRS:
                return MultiQueryParamMode.MULTI_PAIRS;
            case COMMA_SEPARATED:
                return MultiQueryParamMode.COMMA_SEPARATED;
            case ARRAY_PAIRS:
                return MultiQueryParamMode.ARRAY_PAIRS;
        }
        return null;
    }
}
