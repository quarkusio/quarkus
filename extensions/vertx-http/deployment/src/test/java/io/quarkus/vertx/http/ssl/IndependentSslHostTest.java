package io.quarkus.vertx.http.ssl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.HttpServerStart;
import io.quarkus.vertx.http.HttpsServerStart;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class IndependentSslHostTest {

    private static final String configuration = """
            quarkus.http.host=127.0.0.1
            quarkus.http.ssl-host=0.0.0.0
            quarkus.http.ssl.certificate.files=server-cert.pem
            quarkus.http.ssl.certificate.key-files=server-key.pem
            """;

    @TestHTTPResource(value = "/ssl", tls = true)
    URL httpsUrl;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, HostListener.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File("target/certs/ssl-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-test.crt"), "server-cert.pem"));

    @Test
    public void testIndependentSslHost() throws InterruptedException {
        assertThat(HostListener.HTTP.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(HostListener.HTTPS.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(HostListener.httpHost).isEqualTo("127.0.0.1");
        assertThat(HostListener.httpsHost).isEqualTo("0.0.0.0");

        RestAssured.given().get("http://127.0.0.1:8081/ssl").then().statusCode(200).body(is("plain"));
        RestAssured
                .given()
                .trustStore(new File("target/certs/ssl-test-truststore.jks"), "secret")
                .get(httpsUrl).then().statusCode(200).body(is("ssl"));
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/ssl").handler(rc -> {
                if (rc.request().isSSL()) {
                    rc.response().end("ssl");
                } else {
                    rc.response().end("plain");
                }
            });
        }
    }

    @Dependent
    public static class HostListener {

        static final CountDownLatch HTTP = new CountDownLatch(1);
        static final CountDownLatch HTTPS = new CountDownLatch(1);
        static volatile String httpHost;
        static volatile String httpsHost;

        void httpStarted(@ObservesAsync HttpServerStart start) {
            httpHost = start.config().getTcpHost();
            HTTP.countDown();
        }

        void httpsStarted(@ObservesAsync HttpsServerStart start) {
            httpsHost = start.config().getTcpHost();
            HTTPS.countDown();
        }
    }
}
