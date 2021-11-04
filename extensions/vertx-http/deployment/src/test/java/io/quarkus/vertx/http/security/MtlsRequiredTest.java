package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class MtlsRequiredTest {

    @TestHTTPResource(value = "/mtls", ssl = true)
    URL url;

    @TestHTTPResource(value = "/mtls", ssl = false)
    URL urlNoTls;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("src/test/resources/conf/mtls/mtls-jks.conf"), "application.properties")
                    .addAsResource(new File("src/test/resources/conf/mtls/server-keystore.jks"), "server-keystore.jks")
                    .addAsResource(new File("src/test/resources/conf/mtls/server-truststore.jks"), "server-truststore.jks"));

    @Test
    public void testClientAuthentication() {
        RestAssured.given()
                .keyStore(new File("src/test/resources/conf/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/conf/mtls/client-truststore.jks"), "password")
                .get(url).then().statusCode(200).body(is("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
    }

    @Test
    public void testNoClientCert() {
        RestAssured.given()
                .get(urlNoTls).then().statusCode(401);
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/mtls").handler(rc -> {
                rc.response().end(QuarkusHttpUser.class.cast(rc.user()).getSecurityIdentity().getPrincipal().getName());
            });
        }

    }
}
