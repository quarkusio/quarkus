package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.PKCS12 })
})
public class TooManyKeyStoreConfiguredProviderAndOtherTest {

    private static final String configuration = """
            quarkus.tls.key-store.other.type=PKCS12
            quarkus.tls.key-store.other.path=target/certs/test-formats-keystore.p12
            quarkus.tls.key-store.other.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestKeyStoreProvider.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t)
                        .hasMessageContaining(
                                "cannot be configured with a provider and PEM, PKCS12, JKS, or other at the same time");
            });

    @Test
    void shouldNotBeCalled() {
        fail("This test should not be called");
    }

    @ApplicationScoped
    static class TestKeyStoreProvider implements KeyStoreProvider {
        @Override
        public KeyStoreAndKeyCertOptions getKeyStore(Vertx vertx) {
            return null;
        }
    }
}
