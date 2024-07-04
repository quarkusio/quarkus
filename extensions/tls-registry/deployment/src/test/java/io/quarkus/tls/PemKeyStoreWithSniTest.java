package io.quarkus.tls;

import static io.smallrye.certs.Format.PEM;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

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
        @Certificate(name = "test-sni-pem", formats = { PEM }, aliases = {
                @Alias(name = "sni-1", cn = "acme.org"),
                @Alias(name = "sni-2", cn = "example.com"),
        })
})
public class PemKeyStoreWithSniTest {

    private static final String configuration = """
            quarkus.tls.key-store.pem.1.cert=target/certs/sni-1.crt
            quarkus.tls.key-store.pem.1.key=target/certs/sni-1.key
            quarkus.tls.key-store.pem.2.cert=target/certs/sni-2.crt
            quarkus.tls.key-store.pem.2.key=target/certs/sni-2.key
            quarkus.tls.key-store.sni=true
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void test() throws Exception {
        TlsConfiguration tlsConfiguration = registry.getDefault().orElseThrow();
        assertThat(tlsConfiguration.usesSni()).isTrue();
        assertThat(tlsConfiguration.getKeyStore().size()).isEqualTo(2);
    }

}
