package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.UUID;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ReloadWithoutConfigTest {

    private static final String configuration = """
            # No config - overridden in the test
            """;

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testReloading() {
        TlsConfiguration def = registry.getDefault().orElseThrow();
        assertThat(def.reload()).isFalse();
    }

}
