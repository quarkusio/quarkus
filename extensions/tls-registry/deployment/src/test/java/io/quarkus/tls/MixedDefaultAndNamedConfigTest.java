package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-mixed-default", password = "password", formats = { Format.PKCS12 }),
        @Certificate(name = "test-mixed-named", password = "secret", formats = { Format.PKCS12 })
})
public class MixedDefaultAndNamedConfigTest {

    private static final String configuration = """
            # Default configuration
            quarkus.tls.key-store.p12.path=target/certs/test-mixed-default-keystore.p12
            quarkus.tls.key-store.p12.password=password
            quarkus.tls.trust-store.p12.path=target/certs/test-mixed-default-truststore.p12
            quarkus.tls.trust-store.p12.password=password
            quarkus.tls.protocols=TLSv1.3

            # Named configuration 'http'
            quarkus.tls.http.key-store.p12.path=target/certs/test-mixed-named-keystore.p12
            quarkus.tls.http.key-store.p12.password=secret
            quarkus.tls.http.trust-store.p12.path=target/certs/test-mixed-named-truststore.p12
            quarkus.tls.http.trust-store.p12.password=secret
            quarkus.tls.http.protocols=TLSv1.3,TLSv1.2
            quarkus.tls.http.alpn=false

            # Named configuration 'grpc' - trust-all mode
            quarkus.tls.grpc.trust-all=true
            quarkus.tls.grpc.hostname-verification-algorithm=NONE
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void testDefaultAndNamedConfigsAreIndependent() throws KeyStoreException {
        TlsConfiguration defConfig = certificates.getDefault().orElseThrow();
        TlsConfiguration httpConfig = certificates.get("http").orElseThrow();
        TlsConfiguration grpcConfig = certificates.get("grpc").orElseThrow();

        assertThat(defConfig.getKeyStoreOptions()).isNotNull();
        assertThat(defConfig.getTrustStoreOptions()).isNotNull();
        assertThat(defConfig.isTrustAll()).isFalse();
        assertThat(defConfig.getSSLOptions().getEnabledSecureTransportProtocols()).containsExactly("TLSv1.3");
        assertThat(defConfig.getSSLOptions().isUseAlpn()).isTrue();

        assertThat(httpConfig.getKeyStoreOptions()).isNotNull();
        assertThat(httpConfig.getTrustStoreOptions()).isNotNull();
        assertThat(httpConfig.isTrustAll()).isFalse();
        assertThat(httpConfig.getSSLOptions().getEnabledSecureTransportProtocols())
                .containsExactlyInAnyOrder("TLSv1.3", "TLSv1.2");
        assertThat(httpConfig.getSSLOptions().isUseAlpn()).isFalse();

        assertThat(grpcConfig.isTrustAll()).isTrue();
        assertThat(grpcConfig.getHostnameVerificationAlgorithm()).hasValue("NONE");
        assertThat(grpcConfig.getKeyStoreOptions()).isNull();

        assertThat(defConfig.getKeyStore()).isNotSameAs(httpConfig.getKeyStore());
    }
}
