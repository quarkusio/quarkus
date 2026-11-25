package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-passwordless", password = "password", formats = { Format.PEM })
})
public class PasswordlessOtherTrustStoreWithMissingAliasTest {

    private static final Path tempDir = Path.of("target/test-certificates-" + UUID.randomUUID());
    private static final Path trustStorePath = tempDir.resolve("trust.p12");

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(""), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.tls.passwordless.trust-store.other.type", "PKCS12")
            .overrideRuntimeConfigKey("quarkus.tls.passwordless.trust-store.other.path",
                    trustStorePath.toAbsolutePath().toString())
            .overrideRuntimeConfigKey("quarkus.tls.passwordless.trust-store.other.alias", "missing-alias")
            .setBeforeAllCustomizer(() -> {
                try {
                    PasswordlessOtherTrustStoreTest.createPasswordlessPkcs12TrustStore(
                            Path.of("target/certs/test-passwordless.crt"),
                            "test-passwordless",
                            trustStorePath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .assertException(t -> {
                assertThat(t).hasStackTraceContaining(
                        "Alias 'missing-alias' not found in trust store (certificate not found) 'passwordless'");
            });

    @AfterAll
    static void cleanup() throws Exception {
        if (Files.exists(tempDir)) {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    @Test
    void test() {
        fail("Should not be called");
    }
}
