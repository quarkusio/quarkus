package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class JKSTrustStoreWithPathAndValueTest {

    private static final String configuration = """
            quarkus.tls.trust-store.jks.path=target/certs/test-formats-truststore.jks
            quarkus.tls.trust-store.jks.value=dGVzdA==
            quarkus.tls.trust-store.jks.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .assertException(t -> {
                assertThat(t.getMessage()).contains("default", "JKS trust store", "`path` and `value`");
            });

    @Test
    void test() {
        fail("Should not be called as the extension should fail before.");
    }
}
