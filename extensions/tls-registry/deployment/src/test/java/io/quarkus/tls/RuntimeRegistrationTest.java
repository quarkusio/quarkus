package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-registration", password = "password", formats = Format.PKCS12)
})
public class RuntimeRegistrationTest {

    private static final String configuration = """
            # no configuration by default
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testRegistration() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        assertThat(registry.getDefault()).isNotEmpty();
        assertThat(registry.getDefault().orElseThrow().isTrustAll()).isFalse();
        assertThat(registry.get("named")).isEmpty();

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(getClass().getResourceAsStream("target/certs/test-registration-keystore.p12"), "password".toCharArray());
        KeyStore ts = KeyStore.getInstance("PKCS12");
        ts.load(getClass().getResourceAsStream("target/certs/test-registration-truststore.p12"), "password".toCharArray());

        registry.register("named", new BaseTlsConfiguration() {
            @Override
            public KeyStore getKeyStore() {
                return ks;
            }

            @Override
            public KeyStore getTrustStore() {
                return ts;
            }

            @Override
            public boolean isTrustAll() {
                return false;
            }

            @Override
            public String getName() {
                return "test";
            }
        });

        TlsConfiguration conf = registry.get("named").orElseThrow();
        assertThat(conf.getKeyStore()).isSameAs(ks);
        assertThat(conf.getTrustStore()).isSameAs(ts);
    }

    @Test
    void cannotRegisterWithNull() {
        assertThatThrownBy(() -> registry.register(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The name of the TLS configuration to register cannot be null");
    }

    @Test
    void cannotRegisterWithDefault() {
        assertThatThrownBy(() -> registry.register("<default>", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The name of the TLS configuration to register cannot be <default>");
    }

    @Test
    void cannotRegisterNull() {
        assertThatThrownBy(() -> registry.register("foo", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
