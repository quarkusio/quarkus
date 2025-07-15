package io.quarkus.vertx.http.security;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;

import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

public class AuthMechanismConfig {

    void configure(@Observes HttpSecurity httpSecurity, TlsConfigurationRegistry tlsConfigRegistry) {
        httpSecurity
                .mTLS(
                        MTLS.builder()
                                .tls("cert-1")
                                .certificateToRolesMapper(x509Certificate -> "CN=localhost".equals(
                                        x509Certificate.getIssuerX500Principal().getName()) ? Set.of("admin") : Set.of())
                                .build())
                .path("/public").permit()
                .path("/mtls").roles("admin");

        // please note that you can configure your TLS configuration in the 'application.properties' file instead
        tlsConfigRegistry.register("cert-1", new BaseTlsConfiguration() {

            @Override
            public KeyCertOptions getKeyStoreOptions() {
                return new KeyStoreOptions()
                        .setPath("target/certs/mtls-test-keystore.p12")
                        .setPassword("secret")
                        .setType("PKCS12");
            }

            @Override
            public TrustOptions getTrustStoreOptions() {
                return new KeyStoreOptions()
                        .setPath("target/certs/mtls-test-server-truststore.p12")
                        .setPassword("secret")
                        .setType("PKCS12");
            }

            @Override
            public SSLOptions getSSLOptions() {
                SSLOptions options = new SSLOptions();
                options.setKeyCertOptions(getKeyStoreOptions());
                options.setTrustOptions(getTrustStoreOptions());
                options.setSslHandshakeTimeoutUnit(TimeUnit.SECONDS);
                options.setSslHandshakeTimeout(10);
                options.setEnabledSecureTransportProtocols(Set.of("TLSv1.3", "TLSv1.2"));
                return options;
            }
        });
    }

}
