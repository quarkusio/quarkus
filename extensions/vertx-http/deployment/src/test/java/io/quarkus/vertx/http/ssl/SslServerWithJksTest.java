package io.quarkus.vertx.http.ssl;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class SslServerWithJksTest {

    @TestHTTPResource(value = "/ssl", ssl = true)
    URL url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("src/test/resources/conf/ssl-jks.conf"), "application.properties")
                    .addAsResource(new File("src/test/resources/conf/server-keystore.jks"), "server-keystore.jks"));

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @AfterAll
    public static void restoreRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void testSslServerWithJKS() {
        RestAssured.get(url).then().statusCode(200).body(is("ssl"));
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
