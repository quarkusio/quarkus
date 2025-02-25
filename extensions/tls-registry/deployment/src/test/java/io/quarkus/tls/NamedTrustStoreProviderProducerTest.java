package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.runtime.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.TrustStoreProvider;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.net.PemTrustOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class NamedTrustStoreProviderProducerTest {

    private static final String configuration = """
            # no configuration by default
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();
        TlsConfiguration named = certificates.get("http").orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNull();
        assertThat(def.getTrustStore()).isNull();

        assertThat(named.getTrustStoreOptions()).isNotNull();
        assertThat(named.getTrustStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) named.getTrustStore().getCertificate("cert-0");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }

    static class TrustStoreProviderFactory {

        @Produces
        @Identifier("http")
        TrustStoreProvider trustStoreProvider() {
            return vertx -> {
                var options = new PemTrustOptions()
                        .addCertPath("target/certs/test-formats-ca.crt");
                try {
                    return new TrustStoreAndTrustOptions(options.loadKeyStore(vertx), options);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }
}
