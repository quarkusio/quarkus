package io.quarkus.vertx.http.tls;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.net.URL;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.Router;

/**
 * We also set quarkus.http.insecure-requests=disabled in order to test that server starts correctly - see
 * <a href="https://github.com/quarkusio/quarkus/issues/8336">#8336</a>.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class TlsServerWithPemTest {

    private static final String configuration = """
            # Enable SSL, configure the key store
            quarkus.tls.key-store.pem.0.cert=server-cert.pem
            quarkus.tls.key-store.pem.0.key=server-key.pem
            # Test that server starts with this option
            # See https://github.com/quarkusio/quarkus/issues/8336
            quarkus.http.insecure-requests=disabled
            """;

    @TestHTTPResource(value = "/tls", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new StringAsset((configuration)), "application.properties")
                    .addAsResource(new File("target/certs/ssl-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-test.crt"), "server-cert.pem"));

    @Test
    public void testSslServerWithPem() {
        RestAssured
                .given()
                .trustStore(new File("target/certs/ssl-test-truststore.jks"), "secret")
                .get(url).then().statusCode(200).body(is("ssl"));
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/tls").handler(rc -> {
                Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
                Assertions.assertThat(rc.request().isSSL()).isTrue();
                Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();
                rc.response().end("ssl");
            });
        }

    }
}
