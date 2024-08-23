package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.core.Is.is;

import java.io.File;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class DisableHttpPortWithTlsRegistryTest {

    private static final String configuration = """
            # Enable SSL, configure the key store
            quarkus.http.insecure-requests=REDIRECT
            quarkus.tls.key-store.pem.0.cert=server-cert.crt
            quarkus.tls.key-store.pem.0.key=server-key.key
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File("target/certs/ssl-test.key"), "server-key.key")
                    .addAsResource(new File("target/certs/ssl-test.crt"), "server-cert.crt"));

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured
                .trustStore(new File("target/certs/ssl-test-truststore.jks"), "secret");
    }

    @AfterAll
    public static void restoreRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void testDisabledHttpPortForwardsRequest() {
        given().config(newConfig().redirect(redirectConfig().followRedirects(false)))
                .get("/ssl").then().statusCode(301)
                .header("Location", is("https://localhost:8444/ssl"));
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/ssl").handler(rc -> {
                Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
                Assertions.assertThat(rc.request().isSSL()).isTrue();
                Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();
                rc.response().end("ssl");
            });
        }

    }
}
