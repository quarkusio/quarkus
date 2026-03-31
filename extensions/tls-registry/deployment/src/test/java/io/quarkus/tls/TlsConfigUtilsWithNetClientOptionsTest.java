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
import io.vertx.core.net.NetClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-net-client-opts", password = "password", formats = { Format.PKCS12 })
})
public class TlsConfigUtilsWithNetClientOptionsTest {

    private static final String configuration = """
            quarkus.tls.with-https-verif.trust-store.p12.path=target/certs/test-net-client-opts-truststore.p12
            quarkus.tls.with-https-verif.trust-store.p12.password=password
            quarkus.tls.with-https-verif.hostname-verification-algorithm=HTTPS

            quarkus.tls.with-none-verif.trust-all=true
            quarkus.tls.with-none-verif.hostname-verification-algorithm=NONE

            quarkus.tls.no-verif.trust-store.p12.path=target/certs/test-net-client-opts-truststore.p12
            quarkus.tls.no-verif.trust-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void testNetClientWithHttpsHostnameVerification() {
        TlsConfiguration tlsConfig = certificates.get("with-https-verif").orElseThrow();
        NetClientOptions options = new NetClientOptions();
        TlsConfigUtils.configure(options, tlsConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(options.getTrustOptions()).isNotNull();
        assertThat(options.getHostnameVerificationAlgorithm()).isEqualTo("HTTPS");
    }

    @Test
    void testNetClientWithNoneHostnameVerification() {
        TlsConfiguration tlsConfig = certificates.get("with-none-verif").orElseThrow();
        NetClientOptions options = new NetClientOptions();
        TlsConfigUtils.configure(options, tlsConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(options.isTrustAll()).isTrue();
        assertThat(options.getHostnameVerificationAlgorithm()).isEqualTo("NONE");
    }

    @Test
    void testNetClientWithNoHostnameVerification() {
        TlsConfiguration tlsConfig = certificates.get("no-verif").orElseThrow();
        NetClientOptions options = new NetClientOptions();
        TlsConfigUtils.configure(options, tlsConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(tlsConfig.getHostnameVerificationAlgorithm()).isEmpty();
    }
}
