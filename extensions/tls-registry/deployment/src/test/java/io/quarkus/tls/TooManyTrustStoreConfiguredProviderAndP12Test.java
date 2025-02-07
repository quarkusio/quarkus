package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.runtime.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.TrustStoreProvider;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class TooManyTrustStoreConfiguredProviderAndP12Test {

    private static final String configuration = """
            quarkus.tls.trust-store.p12.path=target/certs/test-formats-truststore.p12
            quarkus.tls.trust-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestTrustStoreProvider.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t)
                        .hasMessageContaining("cannot be configured with a provider and PEM or PKCS12 or JKS at the same time");
            });

    @Test
    void shouldNotBeCalled() {
        fail("This test should not be called");
    }

    @ApplicationScoped
    static class TestTrustStoreProvider implements TrustStoreProvider {
        @Override
        public TrustStoreAndTrustOptions getTrustStore(Vertx vertx) {
            // this method should never be called
            return null;
        }
    }
}
