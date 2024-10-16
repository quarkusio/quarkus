package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

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
public class TrustStoreWithMissingSelectedCredentialsProviderTest {

    private static final String configuration = """
            quarkus.tls.foo.trust-store.p12.path=target/certs/test-credentials-provider-truststore.p12
            quarkus.tls.foo.trust-store.credentials-provider.name=tls
            quarkus.tls.foo.trust-store.credentials-provider.bean-name=missing
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(MyCredentialProvider.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t)
                        .hasMessageContaining("Invalid", "trust store", "foo")
                        .hasStackTraceContaining("Unable to find the credentials provider named 'missing'");
            });

    @Test
    void test() {
        fail("This test should not be called");
    }

    @ApplicationScoped
    @Named("my-provider")
    public static class MyCredentialProvider implements CredentialsProvider {

        private final Map<String, Map<String, String>> credentials = Map.of("tls",
                Map.of(CredentialsProvider.PASSWORD_PROPERTY_NAME, "secret123!"));

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            return credentials.get(credentialsProviderName);
        }
    }

    @ApplicationScoped
    public static class MyOtherCredentialProvider implements CredentialsProvider {

        private final Map<String, Map<String, String>> credentials = Map.of("tls",
                Map.of(CredentialsProvider.PASSWORD_PROPERTY_NAME, "wrong"));

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            return credentials.get(credentialsProviderName);
        }
    }
}
