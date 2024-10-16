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
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-alias-jks", password = "password", formats = { Format.JKS }, aliases = {
                @Alias(name = "alias1", password = "alias-password", subjectAlternativeNames = "dns:acme.org"),
                @Alias(name = "alias2", password = "alias-password-2")
        })
})
public class JKSKeyStoreWithMissingAliasPasswordTest {

    private static final String configuration = """
            quarkus.tls.key-store.jks.path=target/certs/test-alias-jks-keystore.jks
            quarkus.tls.key-store.jks.password=password
            quarkus.tls.key-store.jks.alias=missing
            quarkus.tls.key-store.jks.alias-password=alias-password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t).hasMessageContaining("<default>", "password");
            });

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        fail("This test should not be called");
    }
}
