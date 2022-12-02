package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RedirectConfig.redirectConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.core.Is.is;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class DisableHttpPortTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("src/test/resources/conf/disable-http.conf"), "application.properties")
                    .addAsResource(new File("src/test/resources/conf/server-key.pem"), "server-key.pem")
                    .addAsResource(new File("src/test/resources/conf/server-cert.pem"), "server-cert.pem"));

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.useRelaxedHTTPSValidation();
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
