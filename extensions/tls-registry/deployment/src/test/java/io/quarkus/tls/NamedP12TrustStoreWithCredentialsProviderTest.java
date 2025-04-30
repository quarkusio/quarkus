package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-credentials-provider", password = "secret123!", formats = { Format.JKS, Format.PKCS12 })
})
public class NamedP12TrustStoreWithCredentialsProviderTest {

    private static final String configuration = """
            quarkus.tls.foo.trust-store.p12.path=target/certs/test-credentials-provider-truststore.p12
            quarkus.tls.foo.trust-store.credentials-provider.name=tls
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyCredentialProvider.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.get("foo").orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNotNull();
        assertThat(def.getTrustStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getTrustStore().getCertificate("test-credentials-provider");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }

    @ApplicationScoped
    public static class MyCredentialProvider implements CredentialsProvider {

        private final Map<String, Map<String, String>> credentials = Map.of("tls",
                Map.of(CredentialsProvider.PASSWORD_PROPERTY_NAME, "secret123!"));

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            return credentials.get(credentialsProviderName);
        }
    }
}
