package io.quarkus.opentelemetry.runtime.exporter.otlp;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;

/**
 * Applies the TLS and proxy settings of an OTLP exporter to the Vert.x HTTP client options.
 */
class HttpClientOptionsConsumer implements Consumer<HttpClientOptions> {
    private static final Logger LOG = Logger.getLogger(HttpClientOptionsConsumer.class);

    private final OtlpExporterConfig config;
    private final URI baseUri;
    private final Optional<TlsConfiguration> maybeTlsConfiguration;
    private final TlsConfigurationRegistry tlsConfigurationRegistry;

    public HttpClientOptionsConsumer(OtlpExporterConfig config, URI baseUri,
            TlsConfigurationRegistry tlsConfigurationRegistry) {
        this.config = config;
        this.baseUri = baseUri;
        this.maybeTlsConfiguration = TlsConfiguration.from(tlsConfigurationRegistry, config.tlsConfigurationName());
        this.tlsConfigurationRegistry = tlsConfigurationRegistry;
    }

    @Override
    public void accept(HttpClientOptions options) {
        configureTLS(options);
        if (config.proxyOptions().enabled()) {
            configureProxyOptions(options);
        }
    }

    private void configureTLS(HttpClientOptions options) {
        configureKeyCertOptions(options);
        configureTrustOptions(options);

        if (OTelExporterUtil.isHttps(baseUri)) {
            options.setSsl(true);
            options.setUseAlpn(true);
        }

        boolean trustAll = maybeTlsConfiguration.map(TlsConfiguration::isTrustAll).orElseGet(
                new Supplier<>() {
                    @Override
                    public Boolean get() {
                        return tlsConfigurationRegistry.getDefault().map(TlsConfiguration::isTrustAll).orElse(false);
                    }
                });
        if (trustAll) {
            LOG.warn("OpenTelemetry exporter TLS is configured with trustAll=true." +
                    " This disables certificate and hostname verification, making the connection vulnerable to" +
                    " MITM attacks. This should not be used in production.");
            options.setTrustAll(true);
            options.setVerifyHost(false);
        }
    }

    private void configureProxyOptions(HttpClientOptions options) {
        var proxyConfig = config.proxyOptions();
        Optional<String> proxyHost = proxyConfig.host();
        if (proxyHost.isPresent()) {
            ProxyOptions proxyOptions = new ProxyOptions()
                    .setHost(proxyHost.get());
            if (proxyConfig.port().isPresent()) {
                proxyOptions.setPort(proxyConfig.port().getAsInt());
            }
            if (proxyConfig.username().isPresent()) {
                proxyOptions.setUsername(proxyConfig.username().get());
            }
            if (proxyConfig.password().isPresent()) {
                proxyOptions.setPassword(proxyConfig.password().get());
            }
            options.setProxyOptions(proxyOptions);
        } else {
            configureProxyOptionsFromJDKSysProps(options);
        }
    }

    private void configureProxyOptionsFromJDKSysProps(HttpClientOptions options) {
        String proxyHost = options.isSsl()
                ? System.getProperty("https.proxyHost", "none")
                : System.getProperty("http.proxyHost", "none");
        String proxyPortAsString = options.isSsl()
                ? System.getProperty("https.proxyPort", "443")
                : System.getProperty("http.proxyPort", "80");
        int proxyPort = Integer.parseInt(proxyPortAsString);

        if (!"none".equals(proxyHost)) {
            ProxyOptions proxyOptions = new ProxyOptions().setHost(proxyHost).setPort(proxyPort);
            String proxyUser = options.isSsl()
                    ? System.getProperty("https.proxyUser")
                    : System.getProperty("http.proxyUser");
            if (proxyUser != null && !proxyUser.isBlank()) {
                proxyOptions.setUsername(proxyUser);
            }
            String proxyPassword = options.isSsl()
                    ? System.getProperty("https.proxyPassword")
                    : System.getProperty("http.proxyPassword");
            if (proxyPassword != null && !proxyPassword.isBlank()) {
                proxyOptions.setPassword(proxyPassword);
            }
            options.setProxyOptions(proxyOptions);
        }
    }

    private void configureKeyCertOptions(HttpClientOptions options) {
        if (maybeTlsConfiguration.isPresent()) {
            options.setKeyCertOptions(maybeTlsConfiguration.get().getKeyStoreOptions());
            return;
        }

        OtlpExporterTracesConfig.KeyCert keyCert = config.keyCert();
        if (keyCert.certs().isEmpty() && keyCert.keys().isEmpty()) {
            return;
        }

        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
        if (keyCert.certs().isPresent()) {
            for (String cert : keyCert.certs().get()) {
                pemKeyCertOptions.addCertPath(cert);
            }
        }
        if (keyCert.keys().isPresent()) {
            for (String cert : keyCert.keys().get()) {
                pemKeyCertOptions.addKeyPath(cert);
            }
        }
        options.setKeyCertOptions(pemKeyCertOptions);
    }

    private void configureTrustOptions(HttpClientOptions options) {
        if (maybeTlsConfiguration.isPresent()) {
            options.setTrustOptions(maybeTlsConfiguration.get().getTrustStoreOptions());
            return;
        }

        OtlpExporterTracesConfig.TrustCert trustCert = config.trustCert();
        if (trustCert.certs().isPresent()) {
            List<String> certs = trustCert.certs().get();
            if (!certs.isEmpty()) {
                PemTrustOptions pemTrustOptions = new PemTrustOptions();
                for (String cert : trustCert.certs().get()) {
                    pemTrustOptions.addCertPath(cert);
                }
                options.setTrustOptions(pemTrustOptions);
            }
        }
    }
}
