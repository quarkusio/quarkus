package io.quarkus.vertx.http.tls;

import java.io.File;
import java.net.URL;
import java.security.cert.X509Certificate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test-sni", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }, cn = "acme.org", subjectAlternativeNames = "DNS:example.com", aliases = {
                @Alias(name = "alias", cn = "foo.com", subjectAlternativeNames = "DNS:acme.org", password = "secret"),
                @Alias(name = "alias-2", cn = "bar.biz", subjectAlternativeNames = "DNS:localhost", password = "secret")
        }))
public class TlsServerWithP12WithSniMatchingSanDNSTest {

    @TestHTTPResource(value = "/tls", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-test-sni-keystore.p12"), "server-keystore.pkcs12"))
            .overrideConfigKey("quarkus.tls.key-store.p12.path", "server-keystore.pkcs12")
            .overrideConfigKey("quarkus.tls.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.key-store.p12.alias-password", "secret")
            .overrideConfigKey("quarkus.tls.key-store.sni", "true");

    @Inject
    Vertx vertx;

    @Test
    public void testSslServerWithPkcs12() {
        // Cannot use RESTAssured as it does not validate the certificate names (even when forced.)
        WebClientOptions options = new WebClientOptions()
                .setSsl(true)
                .setTrustOptions(new io.vertx.core.net.JksOptions()
                        .setPath("target/certs/ssl-test-sni-truststore.jks")
                        .setPassword("secret"))
                .setForceSni(true);
        WebClient client = WebClient.create(vertx, options);
        HttpResponse<Buffer> response = client.getAbs(url.toExternalForm()).send().toCompletionStage().toCompletableFuture()
                .join();
        Assertions.assertThat(response.statusCode()).isEqualTo(200);
        Assertions.assertThat(response.bodyAsString()).isEqualTo("ssl");
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/tls").handler(rc -> {
                Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
                Assertions.assertThat(rc.request().isSSL()).isTrue();
                Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();

                try {
                    X509Certificate certificate = (X509Certificate) rc.request().connection().sslSession()
                            .getLocalCertificates()[0];
                    Assertions.assertThat(certificate.getIssuerX500Principal().getName()).isEqualTo("CN=bar.biz");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                rc.response().end("ssl");
            });
        }

    }
}
