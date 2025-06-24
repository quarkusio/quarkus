package io.quarkus.rest.client.reactive.runtime;

import static io.quarkus.rest.client.reactive.runtime.Constants.DEFAULT_MAX_CHUNK_SIZE;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.TlsConfig;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.InvalidRestClientDefinitionException;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.jboss.resteasy.reactive.client.handlers.RedirectHandler;
import org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.jaxrs.MultiQueryParamMode;
import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.rest.client.reactive.runtime.ProxyAddressUtil.HostAndPort;
import io.quarkus.restclient.config.RestClientsConfig;
import io.quarkus.tls.TlsConfiguration;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

/**
 * Builder implementation for MicroProfile Rest Client
 */
public class RestClientBuilderImpl implements RestClientBuilder {

    private static final String DEFAULT_MAPPER_DISABLED = "microprofile.rest.client.disable.default.mapper";
    private static final String TLS_TRUST_ALL = "quarkus.tls.trust-all";
    private static final String ENABLE_COMPRESSION = "quarkus.http.enable-compression";

    private final ClientBuilderImpl clientBuilder = (ClientBuilderImpl) new ClientBuilderImpl()
            .withConfig(new ConfigurationImpl(RuntimeType.CLIENT));
    private final List<ResponseExceptionMapper<?>> exceptionMappers = new ArrayList<>();
    private final List<RedirectHandler> redirectHandlers = new ArrayList<>();
    private final List<ParamConverterProvider> paramConverterProviders = new ArrayList<>();

    private URI uri;
    private Boolean followRedirects;
    private QueryParamStyle queryParamStyle;
    private MultivaluedMap<String, Object> headers = new CaseInsensitiveMap<>();

    private String multipartPostEncoderMode;
    private String proxyHost;
    private Integer proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private String nonProxyHosts;

    private ClientLogger clientLogger;
    private LoggingScope loggingScope;
    private Integer loggingBodyLimit;

    private Boolean trustAll;
    private String userAgent;
    private Boolean disableDefaultMapper;
    private Boolean enableCompression;

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

    public RestClientBuilderImpl tlsConfiguration(TlsConfiguration tlsConfiguration) {
        clientBuilder.tlsConfig(new TlsConfig() {
            @Override
            public KeyStore getKeyStore() {
                return tlsConfiguration.getKeyStore();
            }

            @Override
            public KeyCertOptions getKeyStoreOptions() {
                return tlsConfiguration.getKeyStoreOptions();
            }

            @Override
            public KeyStore getTrustStore() {
                return tlsConfiguration.getTrustStore();
            }

            @Override
            public TrustOptions getTrustStoreOptions() {
                return tlsConfiguration.getTrustStoreOptions();
            }

            @Override
            public SSLOptions getSSLOptions() {
                return tlsConfiguration.getSSLOptions();
            }

            @Override
            public SSLContext createSSLContext() throws Exception {
                return tlsConfiguration.createSSLContext();
            }

            @Override
            public Optional<String> getHostnameVerificationAlgorithm() {
                return tlsConfiguration.getHostnameVerificationAlgorithm();
            }

            @Override
            public boolean usesSni() {
                return tlsConfiguration.usesSni();
            }

            @Override
            public boolean isTrustAll() {
                return tlsConfiguration.isTrustAll();
            }

            @Override
            public Optional<String> getName() {
                return Optional.ofNullable(tlsConfiguration.getName());
            }
        });
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

    public RestClientBuilderImpl multipartPostEncoderMode(String mode) {
        this.multipartPostEncoderMode = mode;
        return this;
    }

    public RestClientBuilderImpl clientLogger(ClientLogger clientLogger) {
        this.clientLogger = clientLogger;
        return this;
    }

    public RestClientBuilderImpl loggingScope(LoggingScope loggingScope) {
        this.loggingScope = loggingScope;
        return this;
    }

    public RestClientBuilderImpl loggingBodyLimit(Integer limit) {
        this.loggingBodyLimit = limit;
        return this;
    }

    public RestClientBuilderImpl trustAll(boolean trustAll) {
        this.trustAll = trustAll;
        return this;
    }

    public RestClientBuilderImpl userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public RestClientBuilderImpl disableDefaultMapper(Boolean disableDefaultMapper) {
        this.disableDefaultMapper = disableDefaultMapper;
        return this;
    }

    public RestClientBuilderImpl enableCompression(boolean enableCompression) {
        this.enableCompression = enableCompression;
        return this;
    }

    @Override
    public RestClientBuilderImpl executorService(ExecutorService executor) {
        throw new IllegalArgumentException("Specifying executor service is not supported. " +
                "The underlying call is non-blocking, " +
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
    public RestClientBuilder header(final String name, final Object value) {
        headers.add(Objects.requireNonNull(name, "A header name is required."),
                Objects.requireNonNull(value, "Value for header is required."));
        return this;
    }

    @Override
    public <T> T build(Class<T> aClass) throws IllegalStateException, RestClientDefinitionException {
        ArcContainer arcContainer = Arc.container();
        if (arcContainer == null) {
            throw new IllegalStateException(
                    "The Reactive REST Client needs to be built within the context of a Quarkus application with a valid ArC (CDI) context running.");
        }

        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        RestClientsConfig restClients = config.getConfigMapping(RestClientsConfig.class);

        // support overriding the URI from the override-uri property
        var overrideUrlKeyName = String.format("quarkus.rest-client.\"%s\".override-uri", aClass.getName());
        Optional<String> maybeOverrideUri = config.getOptionalValue(overrideUrlKeyName, String.class);
        if (maybeOverrideUri.isPresent()) {
            uri = URI.create(maybeOverrideUri.get());
        }

        if (uri == null) {
            // mandated by the spec
            throw new IllegalStateException("No URL specified. Cannot build a rest client without URL");
        }

        RestClientListeners.get().forEach(listener -> listener.onNewClient(aClass, this));

        AnnotationRegisteredProviders annotationRegisteredProviders = arcContainer
                .instance(AnnotationRegisteredProviders.class).get();
        for (Map.Entry<Class<?>, Integer> mapper : annotationRegisteredProviders.getProviders(aClass).entrySet()) {
            register(mapper.getKey(), mapper.getValue());
        }

        exceptionMappers.sort(Comparator.comparingInt(ResponseExceptionMapper::getPriority));
        redirectHandlers.sort(Comparator.comparingInt(RedirectHandler::getPriority));
        clientBuilder.register(new MicroProfileRestClientResponseFilter(exceptionMappers));
        clientBuilder.followRedirects(followRedirects != null ? followRedirects : restClients.followRedirects().orElse(false));

        RestClientsConfig.RestClientLoggingConfig configRootLogging = restClients.logging();

        Integer defaultLoggingBodyLimit = 100;
        LoggingScope effectiveLoggingScope = LoggingScope.NONE;
        Integer effectiveLoggingBodyLimit = defaultLoggingBodyLimit;
        if (getConfiguration().hasProperty(QuarkusRestClientProperties.LOGGING_SCOPE)) {
            effectiveLoggingScope = (LoggingScope) getConfiguration().getProperty(QuarkusRestClientProperties.LOGGING_SCOPE);
        } else if (loggingScope != null) { //scope, specified programmatically, takes precedence over global configuration
            effectiveLoggingScope = loggingScope;
        } else if (configRootLogging != null) {
            effectiveLoggingScope = configRootLogging.scope().map(LoggingScope::forName).orElse(LoggingScope.NONE);
        }
        if (getConfiguration().hasProperty(QuarkusRestClientProperties.LOGGING_BODY_LIMIT)) {
            effectiveLoggingBodyLimit = (Integer) getConfiguration()
                    .getProperty(QuarkusRestClientProperties.LOGGING_BODY_LIMIT);
        } else if (loggingBodyLimit != null) { //bodyLimit, specified programmatically, takes precedence over global configuration
            effectiveLoggingBodyLimit = loggingBodyLimit;
        } else if (configRootLogging != null) {
            effectiveLoggingBodyLimit = configRootLogging.bodyLimit();
        }
        clientBuilder.loggingScope(effectiveLoggingScope);
        clientBuilder.loggingBodySize(effectiveLoggingBodyLimit);
        if (clientLogger != null) {
            clientBuilder.clientLogger(clientLogger);
        } else {
            InstanceHandle<ClientLogger> clientLoggerInstance = arcContainer.instance(ClientLogger.class);
            if (clientLoggerInstance.isAvailable()) {
                clientBuilder.clientLogger(clientLoggerInstance.get());
            }
        }

        clientBuilder.multiQueryParamMode(toMultiQueryParamMode(queryParamStyle));
        clientBuilder.register(new DefaultClientHeadersRequestFilter(headers));

        Boolean effectiveTrustAll = trustAll;
        if (effectiveTrustAll == null) {
            effectiveTrustAll = ConfigProvider.getConfig().getOptionalValue(TLS_TRUST_ALL, Boolean.class)
                    .orElse(false);
        }

        clientBuilder.trustAll(effectiveTrustAll);
        restClients.verifyHost().ifPresent(clientBuilder::verifyHost);

        String effectiveUserAgent = userAgent;
        if (effectiveUserAgent != null) {
            clientBuilder.setUserAgent(effectiveUserAgent);
        } else if (restClients.userAgent().isPresent()) { // if config set and client obtained programmatically
            clientBuilder.setUserAgent(restClients.userAgent().get());
        }

        Boolean effectiveDisableDefaultMapper = disableDefaultMapper;
        if (effectiveDisableDefaultMapper == null) {
            var configOpt = ConfigProvider.getConfig().getOptionalValue(DEFAULT_MAPPER_DISABLED, Boolean.class);
            if (configOpt.isEmpty()) {
                // need to support the legacy way where the user does .property("microprofile.rest.client.disable.default.mapper", true)
                var defaultMapperDisabledFromProperty = getConfiguration().getProperty(DEFAULT_MAPPER_DISABLED);
                if (defaultMapperDisabledFromProperty instanceof Boolean b) {
                    effectiveDisableDefaultMapper = b;
                } else {
                    effectiveDisableDefaultMapper = false;
                }
            } else {
                effectiveDisableDefaultMapper = configOpt.get();
            }
        }

        if (!effectiveDisableDefaultMapper) {
            exceptionMappers.add(new DefaultMicroprofileRestClientExceptionMapper());
        }

        Integer maxChunkSize = (Integer) getConfiguration().getProperty(QuarkusRestClientProperties.MAX_CHUNK_SIZE);
        if (maxChunkSize != null) {
            clientBuilder.maxChunkSize(maxChunkSize);
        } else if (restClients.maxChunkSize().isPresent()) {
            clientBuilder.maxChunkSize((int) restClients.maxChunkSize().get().asLongValue());
        } else if (restClients.multipart().maxChunkSize().isPresent()) {
            clientBuilder.maxChunkSize(restClients.multipart().maxChunkSize().getAsInt());
        } else {
            clientBuilder.maxChunkSize(DEFAULT_MAX_CHUNK_SIZE);
        }

        if (getConfiguration().hasProperty(QuarkusRestClientProperties.HTTP2)) {
            clientBuilder.http2((Boolean) getConfiguration().getProperty(QuarkusRestClientProperties.HTTP2));
        } else if (restClients.http2()) {
            clientBuilder.http2(true);
        }

        if (getConfiguration().hasProperty(QuarkusRestClientProperties.HTTP2_UPGRADE_MAX_CONTENT_LENGTH)) {
            clientBuilder.http2UpgradeMaxContentLength(
                    (int) getConfiguration().getProperty(QuarkusRestClientProperties.HTTP2_UPGRADE_MAX_CONTENT_LENGTH));
        } else if (restClients.http2UpgradeMaxContentLength().isPresent()) {
            clientBuilder.http2UpgradeMaxContentLength((int) restClients.http2UpgradeMaxContentLength().get().asLongValue());
        }

        if (getConfiguration().hasProperty(QuarkusRestClientProperties.ALPN)) {
            clientBuilder.alpn((Boolean) getConfiguration().getProperty(QuarkusRestClientProperties.ALPN));
        } else if (restClients.alpn().isPresent()) {
            clientBuilder.alpn(restClients.alpn().get());
        }

        Boolean effectiveEnableCompression = enableCompression;
        if (effectiveEnableCompression == null) {
            if (restClients.enableCompression().isPresent()) {
                effectiveEnableCompression = restClients.enableCompression().get();
            }
        }
        if (effectiveEnableCompression == null) {
            var maybeGlobalEnableCompression = ConfigProvider.getConfig().getOptionalValue(ENABLE_COMPRESSION, Boolean.class);
            if (maybeGlobalEnableCompression.isPresent()) {
                effectiveEnableCompression = maybeGlobalEnableCompression.get();
            }
        }
        if (effectiveEnableCompression != null) {
            clientBuilder.enableCompression(effectiveEnableCompression);
        }

        if (proxyHost != null) {
            configureProxy(proxyHost, proxyPort, proxyUser, proxyPassword, nonProxyHosts);
        } else if (restClients.proxyAddress().isPresent()) {
            HostAndPort globalProxy = ProxyAddressUtil.parseAddress(restClients.proxyAddress().get());
            configureProxy(globalProxy.host, globalProxy.port, restClients.proxyUser().orElse(null),
                    restClients.proxyPassword().orElse(null), restClients.nonProxyHosts().orElse(null));
        }

        if (!clientBuilder.getConfiguration().hasProperty(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE)) {
            PausableHttpPostRequestEncoder.EncoderMode multipartPostEncoderMode = null;
            if (this.multipartPostEncoderMode != null) {
                multipartPostEncoderMode = PausableHttpPostRequestEncoder.EncoderMode
                        .valueOf(this.multipartPostEncoderMode.toUpperCase(Locale.ROOT));
            } else if (restClients.multipartPostEncoderMode().isPresent()) {
                multipartPostEncoderMode = PausableHttpPostRequestEncoder.EncoderMode
                        .valueOf(restClients.multipartPostEncoderMode().get().toUpperCase(Locale.ROOT));
            }
            if (multipartPostEncoderMode != null) {
                clientBuilder.property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE, multipartPostEncoderMode);
            }
        }

        ClientImpl clientImpl = clientBuilder.build();
        TlsConfig tlsConfig = clientBuilder.getTlsConfig();
        if (tlsConfig != null && tlsConfig.getName().isPresent()) {
            RestClientRecorder.registerReloadableHttpClient(tlsConfig.getName().get(), clientImpl.getVertxHttpClient());
        }
        WebTargetImpl target = (WebTargetImpl) clientImpl.target(uri);
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
