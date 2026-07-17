package io.quarkus.it.keycloak;

import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.spiffe.client.WorkloadCertificateDocument;
import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.quarkus.vertx.http.security.MTLS;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.core.net.TrustOptions;

class SpiffeMtlsSetup {

    void configure(@Observes HttpSecurity httpSecurity, SpiffeClient spiffeClient,
            @ConfigProperty(name = "spiffe.mtls.enabled", defaultValue = "false") boolean mtlsEnabled) {
        if (!mtlsEnabled) {
            return;
        }
        WorkloadCertificateDocument cert = spiffeClient.getWorkloadCertificate().await().indefinitely();
        TlsConfiguration tlsConfiguration = new SpiffeTlsConfiguration(cert);
        httpSecurity
                .mTLS(MTLS.request("spiffe-mtls", tlsConfiguration))
                .path("/spiffe/mtls/server").mTLS();
    }

    private static final class SpiffeTlsConfiguration extends BaseTlsConfiguration {

        private final KeyCertOptions keyCertOptions;
        private final TrustOptions trustOptions;

        private SpiffeTlsConfiguration(WorkloadCertificateDocument cert) {
            var keyCert = new PemKeyCertOptions();
            for (String pem : cert.certificateChain().certificateChainPem()) {
                keyCert.addCertValue(Buffer.buffer(pem));
            }
            keyCert.addKeyValue(Buffer.buffer(cert.certificateChain().privateKeyPem()));
            this.keyCertOptions = keyCert;

            var trust = new PemTrustOptions();
            for (String pem : cert.trustBundle().certificateChainPem()) {
                trust.addCertValue(Buffer.buffer(pem));
            }
            this.trustOptions = trust;
        }

        @Override
        public KeyCertOptions getKeyStoreOptions() {
            return keyCertOptions;
        }

        @Override
        public TrustOptions getTrustStoreOptions() {
            return trustOptions;
        }

        @Override
        public ServerSSLOptions getServerSSLOptions() {
            return new ServerSSLOptions()
                    .setKeyCertOptions(keyCertOptions)
                    .setTrustOptions(trustOptions);
        }
    }
}
