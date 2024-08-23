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
public class DefaultPemKeyStoreWithMultipleAliasesTest {

    private static final String configuration = """
            quarkus.tls.key-store.pem.a.key=target/certs/test-alias-pem.key
            quarkus.tls.key-store.pem.a.cert=target/certs/test-alias-pem.crt
            quarkus.tls.key-store.pem.b.key=target/certs/alias1.key
            quarkus.tls.key-store.pem.b.cert=target/certs/alias1.crt
            quarkus.tls.key-store.pem.c.key=target/certs/alias2.key
            quarkus.tls.key-store.pem.c.cert=target/certs/alias2.crt

            quarkus.tls.key-store.pem.order=c,b,a
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

        assertThat(def.getKeyStoreOptions()).isNotNull();
        assertThat(def.getKeyStore()).isNotNull();

        List<X509Certificate> list = new ArrayList<>();
        Iterator<String> iterator = def.getKeyStore().aliases().asIterator();
        while (iterator.hasNext()) {
            list.add((X509Certificate) def.getKeyStore().getCertificate(iterator.next()));
        }
        assertThat(list).hasSize(3);
    }
}
