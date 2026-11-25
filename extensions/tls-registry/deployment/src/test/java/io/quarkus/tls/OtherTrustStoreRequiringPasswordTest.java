package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @io.smallrye.certs.junit5.Certificate(name = "test-formats", password = "password", formats = { Format.PKCS12 })
})
public class OtherTrustStoreRequiringPasswordTest {

    private static final String PROVIDER_NAME = "TestPasswordRequiredProvider";
    private static final String STORE_TYPE = "PASSREQ";
    private static volatile boolean registeredProvider;

    private static final String configuration = """
            quarkus.tls.trust-store.other.type=PASSREQ
            quarkus.tls.trust-store.other.provider=TestPasswordRequiredProvider
            quarkus.tls.trust-store.other.path=target/certs/test-formats-truststore.p12
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .setBeforeAllCustomizer(() -> {
                if (Security.getProvider(PROVIDER_NAME) == null) {
                    registeredProvider = Security.addProvider(new PasswordRequiredProvider()) != -1;
                }
            })
            .assertException(t -> {
                assertThat(t)
                        .hasMessageContaining("Invalid trust store configuration for certificate '<default>'")
                        .hasStackTraceContaining("Trust store password is required by provider");
                assertThat(t.getMessage())
                        .doesNotContain("cannot be retrieved from the credential provider");
            });

    @AfterAll
    static void removeProvider() {
        if (registeredProvider) {
            Security.removeProvider(PROVIDER_NAME);
            registeredProvider = false;
        }
    }

    @Test
    void test() {
        fail("Should not be called");
    }

    public static final class PasswordRequiredProvider extends Provider {
        @SuppressWarnings("deprecation")
        public PasswordRequiredProvider() {
            super(PROVIDER_NAME, 1.0, "Test provider requiring a keystore password");
            put("KeyStore." + STORE_TYPE, PasswordRequiredKeyStoreSpi.class.getName());
        }
    }

    public static final class PasswordRequiredKeyStoreSpi extends KeyStoreSpi {
        @Override
        public void engineLoad(InputStream stream, char[] password) throws IOException {
            if (password == null) {
                throw new IOException("Trust store password is required by provider");
            }
        }

        @Override
        public Key engineGetKey(String alias, char[] password) throws NoSuchAlgorithmException {
            return null;
        }

        @Override
        public Certificate[] engineGetCertificateChain(String alias) {
            return new Certificate[0];
        }

        @Override
        public Certificate engineGetCertificate(String alias) {
            return null;
        }

        @Override
        public Date engineGetCreationDate(String alias) {
            return null;
        }

        @Override
        public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) {
        }

        @Override
        public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) {
        }

        @Override
        public void engineSetCertificateEntry(String alias, Certificate cert) {
        }

        @Override
        public void engineDeleteEntry(String alias) {
        }

        @Override
        public Enumeration<String> engineAliases() {
            return Collections.emptyEnumeration();
        }

        @Override
        public boolean engineContainsAlias(String alias) {
            return false;
        }

        @Override
        public int engineSize() {
            return 0;
        }

        @Override
        public boolean engineIsKeyEntry(String alias) {
            return false;
        }

        @Override
        public boolean engineIsCertificateEntry(String alias) {
            return false;
        }

        @Override
        public String engineGetCertificateAlias(Certificate cert) {
            return null;
        }

        @Override
        public void engineStore(OutputStream stream, char[] password) {
        }
    }
}
