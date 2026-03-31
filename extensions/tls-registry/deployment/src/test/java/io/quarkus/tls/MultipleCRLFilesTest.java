package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class MultipleCRLFilesTest {

    private static final String configuration = """
            quarkus.tls.certificate-revocation-list=src/test/resources/revocations/revoked-cert.der,src/test/resources/revocations/revoked-cert.der
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void testMultipleCRLFiles() {
        TlsConfiguration def = certificates.getDefault().orElseThrow();
        assertThat(def.getSSLOptions().getCrlValues()).hasSize(2);
    }
}
