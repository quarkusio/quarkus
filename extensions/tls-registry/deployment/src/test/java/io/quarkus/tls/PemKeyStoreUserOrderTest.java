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
        @Certificate(name = "test-pem-order", formats = { Format.PEM }, subjectAlternativeNames = "dns:quarkus.io", aliases = {
                @Alias(name = "test-pem-order-alias1", subjectAlternativeNames = "dns:acme.org"),
                @Alias(name = "test-pem-order-alias2", subjectAlternativeNames = "dns:example.com"),
        })
})
public class PemKeyStoreUserOrderTest {

    private static final String configuration = """
            quarkus.tls.key-store.pem.foo.key=target/certs/test-pem-order.key
            quarkus.tls.key-store.pem.foo.cert=target/certs/test-pem-order.crt
            quarkus.tls.key-store.pem.bar.key=target/certs/test-pem-order-alias1.key
            quarkus.tls.key-store.pem.bar.cert=target/certs/test-pem-order-alias1.crt
            quarkus.tls.key-store.pem.baz.key=target/certs/test-pem-order-alias2.key
            quarkus.tls.key-store.pem.baz.cert=target/certs/test-pem-order-alias2.crt

            quarkus.tls.key-store.pem.order=foo,bar,baz
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

        assertThat(def.getKeyStore()).isNotNull();

        List<X509Certificate> list = new ArrayList<>();
        Iterator<String> iterator = def.getKeyStore().aliases().asIterator();
        while (iterator.hasNext()) {
            String alias = iterator.next();
            X509Certificate certificate = (X509Certificate) def.getKeyStore().getCertificate(alias);
            list.add(certificate);

        }
        assertThat(list).hasSize(3);
        assertThat(new ArrayList<>(list.get(0).getSubjectAlternativeNames()).get(0).toString()).contains("quarkus.io");
        assertThat(new ArrayList<>(list.get(1).getSubjectAlternativeNames()).get(0).toString()).contains("acme.org");
        assertThat(new ArrayList<>(list.get(2).getSubjectAlternativeNames()).get(0).toString()).contains("example.com");
    }
}
