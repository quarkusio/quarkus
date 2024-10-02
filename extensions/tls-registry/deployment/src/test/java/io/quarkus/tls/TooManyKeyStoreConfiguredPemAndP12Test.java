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
public class TooManyKeyStoreConfiguredPemAndP12Test {

    private static final String configuration = """
            quarkus.tls.key-store.pem.0.cert=target/certs/test-formats.crt
            quarkus.tls.key-store.pem.0.key=target/certs/test-formats.key
            quarkus.tls.key-store.p12.path=target/certs/test-formats-keystore.p12
            quarkus.tls.key-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t).hasMessageContaining("PEM", "PKCS12");
            });

    @Test
    void shouldNotBeCalled() {
        fail("This test should not be called");
    }
}
