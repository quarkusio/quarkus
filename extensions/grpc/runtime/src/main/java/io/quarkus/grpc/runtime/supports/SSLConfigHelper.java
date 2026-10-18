package io.quarkus.grpc.runtime.supports;

import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.TCPSSLOptions;

public class SSLConfigHelper {

    public static void configurePemTrustOptions(TCPSSLOptions options,
            GrpcClientConfiguration.TlsClientConfig.PemTrustCertConfiguration configuration) {
        if ((configuration.certs().isPresent() && !configuration.certs().get().isEmpty())) {
            ensureTrustOptionsNotSet(options);
            options.setTrustOptions(toPemTrustOptions(configuration));
        }
    }

    private static PemTrustOptions toPemTrustOptions(
            GrpcClientConfiguration.TlsClientConfig.PemTrustCertConfiguration configuration) {
        PemTrustOptions pemTrustOptions = new PemTrustOptions();
        if (configuration.certs().isPresent()) {
            for (String cert : configuration.certs().get()) {
                pemTrustOptions.addCertPath(cert);
            }
        }
        return pemTrustOptions;
    }

    public static void configureJksTrustOptions(TCPSSLOptions options,
            GrpcClientConfiguration.TlsClientConfig.JksConfiguration configuration) {
        if (configuration.path().isPresent()) {
            ensureTrustOptionsNotSet(options);
            options.setTrustOptions(toJksOptions(configuration));
        }
    }

    private static JksOptions toJksOptions(GrpcClientConfiguration.TlsClientConfig.JksConfiguration configuration) {
        JksOptions jksOptions = new JksOptions();
        if (configuration.path().isPresent()) {
            jksOptions.setPath(configuration.path().get());
        }
        if (configuration.password().isPresent()) {
            jksOptions.setPassword(configuration.password().get());
        }
        return jksOptions;
    }

    public static void configurePfxTrustOptions(TCPSSLOptions options,
            GrpcClientConfiguration.TlsClientConfig.PfxConfiguration configuration) {
        if (configuration.path().isPresent()) {
            ensureTrustOptionsNotSet(options);
            options.setTrustOptions(toPfxOptions(configuration));
        }
    }

    private static PfxOptions toPfxOptions(GrpcClientConfiguration.TlsClientConfig.PfxConfiguration configuration) {
        PfxOptions pfxOptions = new PfxOptions();
        if (configuration.path().isPresent()) {
            pfxOptions.setPath(configuration.path().get());
        }
        if (configuration.password().isPresent()) {
            pfxOptions.setPassword(configuration.password().get());
        }
        return pfxOptions;
    }

    private static void ensureTrustOptionsNotSet(TCPSSLOptions options) {
        if (options.getTrustOptions() != null) {
            throw new IllegalArgumentException("Trust options have already been set");
        }
    }

    public static void configurePemKeyCertOptions(TCPSSLOptions options,
            GrpcClientConfiguration.TlsClientConfig.PemKeyCertConfiguration configuration) {
        if (configuration.certs().isPresent() && !configuration.certs().get().isEmpty() && configuration.keys().isPresent()
                && !configuration.keys().get().isEmpty()) {
            ensureKeyCertOptionsNotSet(options);
            options.setKeyCertOptions(toPemKeyCertOptions(configuration));
        }
    }

    private static KeyCertOptions toPemKeyCertOptions(
            GrpcClientConfiguration.TlsClientConfig.PemKeyCertConfiguration configuration) {
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        if (configuration.certs().isPresent()) {
            for (String cert : configuration.certs().get()) {
                pemKeyCertOptions.addCertPath(cert);
            }
        }
        if (configuration.keys().isPresent()) {
            for (String cert : configuration.keys().get()) {
                pemKeyCertOptions.addKeyPath(cert);
            }
        }
        return pemKeyCertOptions;
    }

    public static void configureJksKeyCertOptions(TCPSSLOptions options,
            GrpcClientConfiguration.TlsClientConfig.JksConfiguration configuration) {
        if (configuration.path().isPresent()) {
            ensureKeyCertOptionsNotSet(options);
            options.setKeyCertOptions(toJksOptions(configuration));
        }
    }

    public static void configurePfxKeyCertOptions(TCPSSLOptions options,
            GrpcClientConfiguration.TlsClientConfig.PfxConfiguration configuration) {
        if (configuration.path().isPresent()) {
            ensureKeyCertOptionsNotSet(options);
            options.setKeyCertOptions(toPfxOptions(configuration));
        }
    }

    private static void ensureKeyCertOptionsNotSet(TCPSSLOptions options) {
        if (options.getKeyCertOptions() != null) {
            throw new IllegalArgumentException("Key cert options have already been set");
        }
    }

    private SSLConfigHelper() {
        // Utility
    }
}
