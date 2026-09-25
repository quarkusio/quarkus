package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * A password configured for a key that is not encrypted must be reported as such.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class PemWithPasswordButUnencryptedKeyTest {

    private static final String configuration = """
            quarkus.tls.key-store.pem.foo.cert=target/certs/test-formats.crt
            quarkus.tls.key-store.pem.foo.key=target/certs/test-formats.key
            quarkus.tls.key-store.pem.foo.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t.getMessage()).contains("key/certificate pair", "default");
                assertThat(t).hasStackTraceContaining("not an encrypted PKCS#8 key");
            });

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        fail("Should not be called as the extension should fail before.");
    }
}
