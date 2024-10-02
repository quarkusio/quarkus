package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
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
public class JKSKeyStoreWithOverriddenCredentialsProviderTest {

    private static final String configuration = """
            quarkus.tls.key-store.jks.path=target/certs/test-credentials-provider-keystore.jks
            quarkus.tls.key-store.jks.password=secret123!
            quarkus.tls.key-store.credentials-provider.name=tls
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
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        X509Certificate certificate = (X509Certificate) def.getKeyStore().getCertificate("test-credentials-provider");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }

    @ApplicationScoped
    public static class MyCredentialProvider implements CredentialsProvider {

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            return Collections.emptyMap(); // Called for the alias-password
        }
    }
}
