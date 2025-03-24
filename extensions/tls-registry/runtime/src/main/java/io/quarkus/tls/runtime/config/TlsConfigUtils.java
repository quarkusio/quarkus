package io.quarkus.tls.runtime.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.logging.Logger;

import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TCPSSLOptions;

public class TlsConfigUtils {

    private static final Logger log = Logger.getLogger(TlsConfigUtils.class);

    private TlsConfigUtils() {
        // Avoid direct instantiation
    }

    /**
     * Read the content of the path.
     * <p>
     * The file is read from the classpath if it exists, otherwise it is read from the file system.
     *
     * @param path the path, must not be {@code null}
     * @return the content of the file
     */
    public static byte[] read(Path path) {
        byte[] data;
        try {
            final InputStream resource = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(ClassPathUtils.toResourceName(path));
            if (resource != null) {
                try (InputStream is = resource) {
                    data = is.readAllBytes();
                }
            } else {
                try (InputStream is = Files.newInputStream(path)) {
                    data = is.readAllBytes();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read file " + path, e);
        }
        return data;
    }

    /**
     * Configure the {@link TCPSSLOptions} with the given {@link TlsConfiguration}.
     *
     * @param options the options to configure
     * @param configuration the configuration to use
     */
    public static void configure(TCPSSLOptions options, TlsConfiguration configuration) {
        options.setSsl(true);
        if (configuration.getTrustStoreOptions() != null) {
            options.setTrustOptions(configuration.getTrustStoreOptions());
        }

        // For mTLS:
        if (configuration.getKeyStoreOptions() != null) {
            options.setKeyCertOptions(configuration.getKeyStoreOptions());
        }

        SSLOptions sslOptions = configuration.getSSLOptions();
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
            // Try to set ALPN configuration, but handle UnsupportedOperationException
            // for example, if the underlying implementation does not support it (es. AMQP)
            try {
                options.setUseAlpn(sslOptions.isUseAlpn());
            } catch (UnsupportedOperationException e) {
                log.warnf(
                        "ALPN configuration not supported by implementation: %s. ALPN setting will be ignored.",
                        options.getClass().getName());
            }
        }
    }

    /**
     * Configure the {@link ClientOptionsBase} with the given {@link TlsConfiguration}.
     *
     * @param options the options to configure
     * @param configuration the configuration to use
     */
    public static void configure(ClientOptionsBase options, TlsConfiguration configuration) {
        configure((TCPSSLOptions) options, configuration);
        if (configuration.isTrustAll()) {
            options.setTrustAll(true);
        }
    }

    /**
     * Configure the {@link NetClientOptions} with the given {@link TlsConfiguration}.
     *
     * @param options the options to configure
     * @param configuration the configuration to use
     */
    public static void configure(NetClientOptions options, TlsConfiguration configuration) {
        configure((ClientOptionsBase) options, configuration);
        if (configuration.getHostnameVerificationAlgorithm().isPresent()) {
            options.setHostnameVerificationAlgorithm(configuration.getHostnameVerificationAlgorithm().get());
        }
    }

    /**
     * Configure the {@link HttpClientOptions} with the given {@link TlsConfiguration}.
     *
     * @param options the options to configure
     * @param configuration the configuration to use
     */
    public static void configure(HttpClientOptions options, TlsConfiguration configuration) {
        configure((ClientOptionsBase) options, configuration);
        options.setForceSni(configuration.usesSni());
        if (configuration.getHostnameVerificationAlgorithm().isPresent()
                && configuration.getHostnameVerificationAlgorithm().get().equals("NONE")) {
            // Only disable hostname verification if the algorithm is explicitly set to NONE
            options.setVerifyHost(false);
        }
    }

    /**
     * Configure the {@link WebSocketClientOptions} with the given {@link TlsConfiguration}.
     *
     * @param options the options to configure
     * @param configuration the configuration to use
     */
    public static void configure(WebSocketClientOptions options, TlsConfiguration configuration) {
        configure((ClientOptionsBase) options, configuration);
        if (configuration.getHostnameVerificationAlgorithm().isPresent()
                && configuration.getHostnameVerificationAlgorithm().get().equals("NONE")) {
            // Only disable hostname verification if the algorithm is explicitly set to NONE
            options.setVerifyHost(false);
        }
    }

}
