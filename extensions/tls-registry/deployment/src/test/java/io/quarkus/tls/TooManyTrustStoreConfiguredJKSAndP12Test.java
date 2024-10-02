package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class TooManyTrustStoreConfiguredJKSAndP12Test {

    private static final String configuration = """
            quarkus.tls.trust-store.jks.path=target/certs/test-formats-truststore.jks
            quarkus.tls.trust-store.jks.password=password
            quarkus.tls.trust-store.p12.path=target/certs/test-formats-truststore.p12
            quarkus.tls.trust-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t).hasMessageContaining("PKCS12", "JKS", "trust");
            });

    @Test
    void shouldNotBeCalled() {
        fail("This test should not be called");
    }
}
