package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Produces;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.runtime.KeyStoreAndKeyCertOptions;
import io.quarkus.tls.runtime.KeyStoreProvider;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class AmbiguousNamedKeyStoreProviderTest {

    private static final String configuration = """
            # no configuration by default
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t).isInstanceOf(AmbiguousResolutionException.class);
            });

    @Test
    void shouldNotBeCalled() {
        fail("This test should not be called");
    }

    static class KeyStoreProviderFactory {

        @Produces
        @Identifier("http")
        KeyStoreProvider keyStoreProvider() {
            // this method should never be called
            return null;
        }
    }

    @ApplicationScoped
    @Identifier("http")
    static class TestKeyStoreProvider implements KeyStoreProvider {

        @Override
        public KeyStoreAndKeyCertOptions getKeyStore(Vertx vertx) {
            // this method should never be called
            return null;
        }
    }
}
