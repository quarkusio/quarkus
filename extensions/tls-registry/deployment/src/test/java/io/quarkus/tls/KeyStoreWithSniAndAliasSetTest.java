package io.quarkus.tls;

import static io.smallrye.certs.Format.PKCS12;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.security.KeyStoreException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-sni-p12", password = "sni", formats = { PKCS12 }, aliases = {
                @Alias(name = "sni-1", password = "sni", cn = "acme.org"),
                @Alias(name = "sni-2", password = "sni", cn = "example.com"),
        })
})
public class KeyStoreWithSniAndAliasSetTest {

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=target/certs/test-sni-p12-keystore.p12
            quarkus.tls.key-store.p12.password=sni
            quarkus.tls.key-store.p12.alias-password=sni
            quarkus.tls.key-store.p12.alias=sni-1
            quarkus.tls.key-store.sni=true
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> assertThat(t).hasMessageContaining("alias", "sni"));

    @Test
    void test() throws KeyStoreException {
        fail("Should not be called as the deployment should fail due to the alias in the configuration.");
    }

}
