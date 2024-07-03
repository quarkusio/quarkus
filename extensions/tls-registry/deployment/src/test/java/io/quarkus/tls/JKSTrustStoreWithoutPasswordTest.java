package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

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
public class JKSTrustStoreWithoutPasswordTest {

    private static final String configuration = """
            quarkus.tls.trust-store.jks.path=target/certs/test-credentials-provider-truststore.jks
            quarkus.tls.trust-store.credentials-provider.name=tls
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyCredentialProvider.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t).hasStackTraceContaining(
                        "the trust store password is not set and cannot be retrieved from the credential provider");
            });

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        fail("Should not be called");
    }

    @ApplicationScoped
    public static class MyCredentialProvider implements CredentialsProvider {

        private final Map<String, Map<String, String>> credentials = Map.of("tls", Map.of());

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            return credentials.get(credentialsProviderName);
        }
    }
}
