package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStore;
import java.security.KeyStoreException;

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

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-reg-override", password = "password", formats = Format.PKCS12)
})
public class RuntimeRegistrationOverrideTest {

    private static final String configuration = """
            # Named config that will be overridden at runtime
            quarkus.tls.my-config.key-store.p12.path=target/certs/test-reg-override-keystore.p12
            quarkus.tls.my-config.key-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testRuntimeOverrideOfNamedConfig() throws KeyStoreException {
        TlsConfiguration original = registry.get("my-config").orElseThrow();
        assertThat(original.getKeyStore()).isNotNull();

        KeyStore emptyKs = KeyStore.getInstance("PKCS12");
        try {
            emptyKs.load(null, "password".toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        registry.register("my-config", new BaseTlsConfiguration() {
            @Override
            public KeyStore getKeyStore() {
                return emptyKs;
            }

            @Override
            public String getName() {
                return "my-config";
            }
        });

        TlsConfiguration overridden = registry.get("my-config").orElseThrow();
        assertThat(overridden.getKeyStore()).isSameAs(emptyKs);
        assertThat(overridden.getKeyStore().size()).isEqualTo(0);
    }
}
