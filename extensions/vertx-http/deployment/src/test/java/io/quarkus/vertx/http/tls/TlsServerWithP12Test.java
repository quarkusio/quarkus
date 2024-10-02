package io.quarkus.vertx.http.tls;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.net.URL;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class TlsServerWithP12Test {

    @TestHTTPResource(value = "/tls", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.p12"), "server-keystore.pkcs12"))
            .overrideConfigKey("quarkus.tls.key-store.p12.path", "server-keystore.pkcs12")
            .overrideConfigKey("quarkus.tls.key-store.p12.password", "secret");

    @Test
    public void testSslServerWithPkcs12() {
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
