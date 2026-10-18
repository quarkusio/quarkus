package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.WebSocketClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-ws-client-opts", password = "password", formats = { Format.PKCS12 })
})
public class TlsConfigUtilsWithWebSocketClientOptionsTest {

    private static final String configuration = """
            quarkus.tls.ws-none.trust-all=true
            quarkus.tls.ws-none.hostname-verification-algorithm=NONE

            quarkus.tls.ws-https.trust-store.p12.path=target/certs/test-ws-client-opts-truststore.p12
            quarkus.tls.ws-https.trust-store.p12.password=password
            quarkus.tls.ws-https.hostname-verification-algorithm=HTTPS
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void testWebSocketClientWithNoneVerification() {
        TlsConfiguration tlsConfig = certificates.get("ws-none").orElseThrow();
        WebSocketClientOptions options = new WebSocketClientOptions();
        TlsConfigUtils.configure(options, tlsConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(options.isTrustAll()).isTrue();
        assertThat(options.isVerifyHost()).isFalse();
    }

    @Test
    void testWebSocketClientWithHttpsVerification() {
        TlsConfiguration tlsConfig = certificates.get("ws-https").orElseThrow();
        WebSocketClientOptions options = new WebSocketClientOptions();
        TlsConfigUtils.configure(options, tlsConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(options.getTrustOptions()).isNotNull();
        assertThat(options.isVerifyHost()).isTrue();
    }
}
