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

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats-encrypted-pem", password = "password", formats = { Format.JKS, Format.ENCRYPTED_PEM,
                Format.PKCS12 })
})
public class EncryptedPemWithWrongPasswordTest {

    private static final String configuration = """
            quarkus.tls.key-store.pem.foo.cert=target/certs/test-formats-encrypted-pem.crt
            quarkus.tls.key-store.pem.foo.key=target/certs/test-formats-encrypted-pem.key
            quarkus.tls.key-store.pem.foo.password=wrong
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> assertThat(t.getMessage()).contains("key/certificate pair", "default"));

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        fail("Should not be called as the extension should fail before.");
    }
}
