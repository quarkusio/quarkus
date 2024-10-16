package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collections;
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
public class TrustStoreWithMissingKeyCredentialsProviderTest {

    private static final String configuration = """
            quarkus.tls.foo.trust-store.p12.path=target/certs/test-credentials-provider-truststore.p12
            quarkus.tls.foo.trust-store.credentials-provider.name=tls
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t)
                        .hasMessageContaining("Invalid", "trust store", "foo")
                        .hasStackTraceContaining("the trust store password");
            });

    @Test
    void test() {
        fail("This test should not be called");
    }

    @ApplicationScoped
    public static class MyCredentialProvider implements CredentialsProvider {

        @Override
        public Map<String, String> getCredentials(String credentialsProviderName) {
            return Collections.emptyMap();
        }
    }
}
