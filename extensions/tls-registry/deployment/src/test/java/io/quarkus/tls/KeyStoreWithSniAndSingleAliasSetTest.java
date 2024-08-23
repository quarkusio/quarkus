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
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-sni-single", password = "sni", formats = { PKCS12 })
})
public class KeyStoreWithSniAndSingleAliasSetTest {

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=target/certs/test-sni-single-keystore.p12
            quarkus.tls.key-store.p12.password=sni
            quarkus.tls.key-store.sni=true
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> assertThat(t).hasMessageContaining("alias", "sni"));

    @Test
    void test() throws KeyStoreException {
        fail("Should not be called as the deployment should fail.");
    }

}
