package io.quarkus.tls;

import static org.junit.jupiter.api.Assertions.fail;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * Verify that is trust all is set, trust store is not set.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class TrustAllWithTrustStoreTest {

    private static final String configuration = """
                quarkus.tls.http.trust-all=true
                quarkus.tls.http.trust-store.jks.path=target/certs/test-formats-truststore.jks
                quarkus.tls.http.trust-store.jks.password=password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> Assertions.assertThat(t).hasMessageContaining("trust-all", "trust-store"));

    @Test
    void test() {
        fail("This test should not be called");
    }

}
