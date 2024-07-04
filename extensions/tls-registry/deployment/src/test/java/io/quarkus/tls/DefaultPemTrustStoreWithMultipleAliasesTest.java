package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-alias-pem", formats = { Format.PEM }, aliases = {
                @Alias(name = "alias1", subjectAlternativeNames = "dns:acme.org"),
                @Alias(name = "alias2")
        })
})
public class DefaultPemTrustStoreWithMultipleAliasesTest {

    private static final String configuration = """
            quarkus.tls.trust-store.pem.certs=target/certs/test-alias-pem-ca.crt,target/certs/alias1-ca.crt,target/certs/alias2-ca.crt
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNotNull();
        assertThat(def.getTrustStore()).isNotNull();

        List<X509Certificate> list = new ArrayList<>();
        Iterator<String> iterator = def.getTrustStore().aliases().asIterator();
        while (iterator.hasNext()) {
            list.add((X509Certificate) def.getTrustStore().getCertificate(iterator.next()));
        }
        assertThat(list).hasSize(3);
    }
}
