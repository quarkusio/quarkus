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
        @Certificate(name = "test-alias-p12", password = "password", formats = { Format.PKCS12 }, aliases = {
                @Alias(name = "alias1", password = "alias-password", subjectAlternativeNames = "dns:acme.org"),
                @Alias(name = "alias2", password = "alias-password-2", subjectAlternativeNames = "dns:example.com") })
})
public class P12KeyStoreWithWrongAliasPasswordTest {

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=target/certs/test-alias-p12-keystore.p12
            quarkus.tls.key-store.p12.password=password
            quarkus.tls.key-store.p12.alias=alias1
            quarkus.tls.key-store.p12.alias-password=wrong-password
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
