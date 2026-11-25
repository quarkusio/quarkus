package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
        @Certificate(name = "test-missing-password", password = "password", formats = { Format.PKCS12 })
})
public class OtherTrustStoreWithMissingPasswordTest {

    // The trust store is password-protected, loading it without password returns an empty store
    private static final String configuration = """
            quarkus.tls.trust-store.other.type=PKCS12
            quarkus.tls.trust-store.other.path=target/certs/test-missing-password-truststore.p12
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> assertThat(t).hasStackTraceContaining(
                    "the trust store is empty. If it is password-protected, configure the password."));

    @Test
    void test() {
        fail("Should not be called");
    }
}
