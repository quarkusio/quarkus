package io.quarkus.vertx.http;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.vertx.ext.web.Router;

public class DisableHttpPortTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyBean.class)
                    .addAsResource(new File("src/test/resources/conf/disable-http.conf"), "application.properties")
                    .addAsResource(new File("src/test/resources/conf/server-keystore.jks"), "server-keystore.jks"));

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @AfterAll
    public static void restoreRestAssured() {
        RestAssured.reset();
    }

    @Disabled
    @Test
    public void testDisabledHttpPortForwardsRequest() throws MalformedURLException {
        URL url = new URL("http://localhost:8081/ssl");
        RestAssured.config().getRedirectConfig().followRedirects(false);
        ValidatableResponse response = RestAssured.get(url).then().statusCode(301).body(is("ssl"));
        response.header("Location", "https://localhost:8443/ssl");
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
