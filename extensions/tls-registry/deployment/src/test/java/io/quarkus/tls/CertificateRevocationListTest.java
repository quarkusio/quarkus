package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CertificateRevocationListTest {

    private static final String configuration = """
            quarkus.tls.certificate-revocation-list=src/test/resources/revocations/revoked-cert.der
            quarkus.tls.foo.certificate-revocation-list=src/test/resources/revocations/revoked-cert.der
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() {
        TlsConfiguration def = certificates.getDefault().orElseThrow();
        TlsConfiguration foo = certificates.get("foo").orElseThrow();
        assertThat(def.getSSLOptions().getCrlValues()).hasSize(1);
        assertThat(foo.getSSLOptions().getCrlValues()).hasSize(1);
    }

}
