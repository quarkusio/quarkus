package org.jboss.resteasy.reactive.client.impl;

import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.CONNECT_TIMEOUT;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.READ_TIMEOUT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.TlsConfig;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.logging.DefaultClientLogger;
import org.jboss.resteasy.reactive.client.spi.ClientContextResolver;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.jaxrs.MultiQueryParamMode;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.SSLOptions;

public class ClientBuilderImpl extends ClientBuilder {

    private static final Logger log = Logger.getLogger(ClientBuilderImpl.class);

    private static final ClientContextResolver CLIENT_CONTEXT_RESOLVER = ClientContextResolver.getInstance();
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];
    public static final String PIPE = Pattern.quote("|");

    private ConfigurationImpl configuration;
    private String proxyHost;
    private int proxyPort;
    private String proxyPassword;
    private String proxyUser;
    private String nonProxyHosts;

    private boolean followRedirects;

    private boolean http2;
    private boolean alpn;

    // security settings
    private KeyStore keyStore;
    private char[] keystorePassword;
    private KeyStore trustStore;
    private char[] trustStorePassword;
    private boolean trustAll;
    private boolean verifyHost = true;
    // overridden security settings
    private TlsConfig tlsConfig;

    private ClientLogger clientLogger = new DefaultClientLogger();
    private LoggingScope loggingScope = LoggingScope.NONE;
    private Integer loggingBodySize = 100;

    private int maxChunkSize = 8096;
    private MultiQueryParamMode multiQueryParamMode;

    private String userAgent = RestClientRequestContext.DEFAULT_USER_AGENT_VALUE;

    private Boolean enableCompression;
    private Integer http2UpgradeMaxContentLength;

    public ClientBuilderImpl() {
        configuration = new ConfigurationImpl(RuntimeType.CLIENT);
    }

    @Override
    public ClientBuilder withConfig(Configuration config) {
        this.configuration = new ConfigurationImpl(config);
        return this;
    }

    public ClientBuilder tlsConfig(TlsConfig tlsConfig) {
        this.tlsConfig = tlsConfig;
        return this;
    }

    @Override
    public ClientBuilder sslContext(SSLContext sslContext) {
        // TODO
        throw new UnsupportedOperationException("Specifying SSLContext is not supported at the moment");
    }

    @Override
    public ClientBuilder keyStore(KeyStore keyStore, char[] password) {
        this.keyStore = keyStore;
        this.keystorePassword = password;
        return this;
    }

    @Override
    public ClientBuilder trustStore(KeyStore trustStore) {
        return trustStore(trustStore, null);
    }

    public ClientBuilder trustStore(KeyStore trustStore, char[] password) {
        this.trustStore = trustStore;
        this.trustStorePassword = password;
        return this;
    }

    @Override
    public ClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        // TODO
        throw new UnsupportedOperationException("Specifying HostnameVerifier is not supported at the moment");
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
        configuration.property(CONNECT_TIMEOUT, (int) unit.toMillis(timeout));
        return this;
    }

    @Override
    public ClientBuilder readTimeout(long timeout, TimeUnit unit) {
        configuration.property(READ_TIMEOUT, unit.toMillis(timeout));
        return this;
    }

    public ClientBuilder http2(boolean http2) {
        this.http2 = http2;
        return this;
    }

    public ClientBuilder http2UpgradeMaxContentLength(int http2UpgradeMaxContentLength) {
        this.http2UpgradeMaxContentLength = http2UpgradeMaxContentLength;
        return this;
    }

    public ClientBuilder alpn(boolean alpn) {
        this.alpn = alpn;
        return this;
    }

    public ClientBuilder proxy(String proxyHost, int proxyPort) {
        this.proxyPort = proxyPort;
        this.proxyHost = proxyHost;
        return this;
    }

    public ClientBuilder proxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public ClientBuilder proxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        return this;
    }

    public ClientBuilder followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public ClientBuilder multiQueryParamMode(MultiQueryParamMode multiQueryParamMode) {
        this.multiQueryParamMode = multiQueryParamMode;
        return this;
    }

    public ClientBuilder loggingScope(LoggingScope loggingScope) {
        this.loggingScope = loggingScope;
        return this;
    }

    public ClientBuilder loggingBodySize(Integer loggingBodySize) {
        this.loggingBodySize = loggingBodySize;
        return this;
    }

    public ClientBuilder clientLogger(ClientLogger clientLogger) {
        this.clientLogger = clientLogger;
        return this;
    }

    public ClientBuilder enableCompression(boolean enableCompression) {
        this.enableCompression = enableCompression;
        return this;
    }

    public ClientBuilder maxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return this;
    }

    public TlsConfig getTlsConfig() {
        return tlsConfig;
    }

    @Override
    public ClientImpl build() {
        HttpClientOptions options = Optional.ofNullable(configuration.getFromContext(HttpClientOptions.class))
                .orElseGet(HttpClientOptions::new);
        if (http2) {
            options.setProtocolVersion(HttpVersion.HTTP_2);
        }

        if (http2 || alpn) {
            options.setUseAlpn(true);
            options.setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
        }

        if (http2UpgradeMaxContentLength != null) {
            options.setHttp2UpgradeMaxContentLength(http2UpgradeMaxContentLength);
        }

        if (tlsConfig != null) {
            populateSecurityOptionsFromTlsConfig(options);
        } else {
            populateSecurityOptionsFromExplicitTlsProperties(options);
        }

        if (proxyHost != null) {
            if (!"none".equals(proxyHost)) {
                ProxyOptions proxyOptions = new ProxyOptions()
                        .setHost(proxyHost)
                        .setPort(proxyPort);
                if (proxyPassword != null && !proxyPassword.isBlank()) {
                    proxyOptions.setPassword(proxyPassword);
                }
                if (proxyUser != null && !proxyUser.isBlank()) {
                    proxyOptions.setUsername(proxyUser);
                }
                options.setProxyOptions(proxyOptions);
                configureNonProxyHosts(options, nonProxyHosts);
            }
        } else {
            String proxyHost = options.isSsl()
                    ? System.getProperty("https.proxyHost", "none")
                    : System.getProperty("http.proxyHost", "none");
            String proxyPortAsString = options.isSsl()
                    ? System.getProperty("https.proxyPort", "443")
                    : System.getProperty("http.proxyPort", "80");
            String nonProxyHosts = options.isSsl()
                    ? System.getProperty("https.nonProxyHosts", "localhost|127.*|[::1]")
                    : System.getProperty("http.nonProxyHosts", "localhost|127.*|[::1]");
            int proxyPort = Integer.parseInt(proxyPortAsString);

            if (!"none".equals(proxyHost)) {
                ProxyOptions proxyOptions = new ProxyOptions().setHost(proxyHost).setPort(proxyPort);
                proxyUser = options.isSsl()
                        ? System.getProperty("https.proxyUser")
                        : System.getProperty("http.proxyUser");
                if (proxyUser != null && !proxyUser.isBlank()) {
                    proxyOptions.setUsername(proxyUser);
                }
                proxyPassword = options.isSsl()
                        ? System.getProperty("https.proxyPassword")
                        : System.getProperty("http.proxyPassword");
                if (proxyPassword != null && !proxyPassword.isBlank()) {
                    proxyOptions.setPassword(proxyPassword);
                }
                options.setProxyOptions(proxyOptions);
                if (nonProxyHosts != null) {
                    configureNonProxyHosts(options, nonProxyHosts);
                }
            }
        }

        if (Boolean.TRUE.equals(enableCompression)) {
            options.setDecompressionSupported(true);
        }

        clientLogger.setBodySize(loggingBodySize);

        options.setMaxChunkSize(maxChunkSize);
        return new ClientImpl(options,
                new ConfigurationImpl(configuration),
                CLIENT_CONTEXT_RESOLVER.resolve(Thread.currentThread().getContextClassLoader()),
                null,
                null,
                followRedirects,
                multiQueryParamMode,
                loggingScope,
                clientLogger, userAgent);

    }

    private void populateSecurityOptionsFromTlsConfig(HttpClientOptions options) {
        options.setSsl(true);

        if (tlsConfig.getTrustStoreOptions() != null) {
            options.setTrustOptions(tlsConfig.getTrustStoreOptions());
        }

        // For mTLS:
        if (tlsConfig.getKeyStoreOptions() != null) {
            options.setKeyCertOptions(tlsConfig.getKeyStoreOptions());
        }

        if (tlsConfig.isTrustAll()) {
            options.setTrustAll(true);
        }
        if (tlsConfig.getHostnameVerificationAlgorithm().isPresent()
                && tlsConfig.getHostnameVerificationAlgorithm().get().equals("NONE")) {
            // Only disable hostname verification if the algorithm is explicitly set to NONE
            options.setVerifyHost(false);
        }

        SSLOptions sslOptions = tlsConfig.getSSLOptions();
        if (sslOptions != null) {
            options.setSslHandshakeTimeout(sslOptions.getSslHandshakeTimeout());
            options.setSslHandshakeTimeoutUnit(sslOptions.getSslHandshakeTimeoutUnit());
            for (String suite : sslOptions.getEnabledCipherSuites()) {
                options.addEnabledCipherSuite(suite);
            }
            for (Buffer buffer : sslOptions.getCrlValues()) {
                options.addCrlValue(buffer);
            }
            options.setEnabledSecureTransportProtocols(sslOptions.getEnabledSecureTransportProtocols());
            options.setUseAlpn(sslOptions.isUseAlpn());
        }
    }

    private void populateSecurityOptionsFromExplicitTlsProperties(HttpClientOptions options) {
        options.setVerifyHost(verifyHost);
        if (trustAll) {
            options.setTrustAll(true);
            options.setVerifyHost(false);
        }

        char[] effectiveTrustStorePassword = trustStorePassword == null ? EMPTY_CHAR_ARRAY : trustStorePassword;
        Buffer keyStore = asBuffer(this.keyStore, keystorePassword);
        Buffer trustStore = asBuffer(this.trustStore, effectiveTrustStorePassword);
        if (keyStore != null || trustStore != null) {
            options.setSsl(true);
            if (keyStore != null) {
                JksOptions jks = new JksOptions();
                jks.setValue(keyStore);
                jks.setPassword(new String(keystorePassword));
                options.setKeyStoreOptions(jks);
            }
            if (trustStore != null) {
                JksOptions jks = new JksOptions();
                jks.setValue(trustStore);
                jks.setPassword(new String(effectiveTrustStorePassword));
                options.setTrustStoreOptions(jks);
            }
        }
    }

    private void configureNonProxyHosts(HttpClientOptions options, String nonProxyHosts) {
        if (nonProxyHosts != null) {
            for (String host : nonProxyHosts.split(PIPE)) {
                if (!host.isBlank()) {
                    options.addNonProxyHost(host);
                }
            }
        }
    }

    private Buffer asBuffer(KeyStore keyStore, char[] password) {
        if (keyStore != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                keyStore.store(out, password);
                return Buffer.buffer(out.toByteArray());
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                log.error("Failed to translate keystore to vert.x keystore", e);
            }
        }
        return null;
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

    /*
     * Add some custom methods that allow registering MessageBodyReader and MessageBodyWriter classes with all the necessary
     * information
     */

    public ClientBuilderImpl registerMessageBodyReader(MessageBodyReader<?> reader, Class<?> handledType, List<String> consumes,
            boolean builtin, Integer priority) {
        configuration.registerMessageBodyReader(reader, handledType, consumes, RuntimeType.CLIENT, builtin, priority);
        return this;
    }

    public ClientBuilderImpl registerMessageBodyWriter(MessageBodyWriter<?> writer, Class<?> handledType, List<String> consumes,
            boolean builtin, Integer priority) {
        configuration.registerMessageBodyWriter(writer, handledType, consumes, RuntimeType.CLIENT, builtin, priority);
        return this;
    }

    public ClientBuilderImpl trustAll(boolean trustAll) {
        this.trustAll = trustAll;
        return this;
    }

    public ClientBuilderImpl verifyHost(boolean verifyHost) {
        this.verifyHost = verifyHost;
        return this;
    }

    public ClientBuilderImpl setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public ClientBuilderImpl nonProxyHosts(String nonProxyHosts) {
        this.nonProxyHosts = nonProxyHosts;
        return this;
    }
}
