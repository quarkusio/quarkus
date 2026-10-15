package io.quarkus.vertx.http.tls;

import java.io.File;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test-sni-alias", password = "secret", formats = {
        Format.PKCS12 }, aliases = {
                @Alias(name = "sni-1", password = "secret", cn = "acme.org", subjectAlternativeNames = "DNS:acme.org"),
                @Alias(name = "sni-2", password = "secret", cn = "example.com", subjectAlternativeNames = "DNS:example.com"),
        }))
class TlsServerWithSniAndExplicitAliasTest extends AbstractTlsServerWithSniTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-test-sni-alias-keystore.p12"), "server-keystore.pkcs12"))
            .overrideConfigKey("quarkus.tls.key-store.p12.path", "server-keystore.pkcs12")
            .overrideConfigKey("quarkus.tls.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.key-store.p12.alias-password", "secret")
            .overrideConfigKey("quarkus.tls.key-store.p12.alias", "sni-1")
            .overrideConfigKey("quarkus.tls.key-store.sni", "true");

    @Override
    String expectedCertCn() {
        return "acme.org";
    }
}
