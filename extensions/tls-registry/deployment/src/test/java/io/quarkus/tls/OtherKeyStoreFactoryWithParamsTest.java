package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.PEM })
})
public class OtherKeyStoreFactoryWithParamsTest {

    private static final String configuration = """
            quarkus.tls.key-store.other.type=test-params
            quarkus.tls.key-store.other.params.cert-name=test-formats
            quarkus.tls.key-store.other.params.base-dir=target/certs
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ParamsKeyStoreFactory.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getKeyStoreOptions()).isNotNull();
        assertThat(def.getKeyStore()).isNotNull();
    }

    @ApplicationScoped
    @Identifier("test-params")
    public static class ParamsKeyStoreFactory implements KeyStoreFactory {
        @Override
        public KeyStoreAndKeyCertOptions createKeyStore(OtherKeyStoreConfiguration config, Vertx vertx, String name) {
            // Verify params are passed through
            String certName = config.params().get("cert-name");
            String baseDir = config.params().get("base-dir");
            assertThat(certName).isEqualTo("test-formats");
            assertThat(baseDir).isEqualTo("target/certs");

            var options = new PemKeyCertOptions()
                    .addCertPath(baseDir + "/" + certName + ".crt")
                    .addKeyPath(baseDir + "/" + certName + ".key");
            try {
                return new KeyStoreAndKeyCertOptions(options.loadKeyStore(vertx), options);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
