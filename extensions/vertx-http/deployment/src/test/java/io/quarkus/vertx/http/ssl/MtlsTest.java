package io.quarkus.vertx.http.ssl;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class MtlsTest {

    @TestHTTPResource(value = "/mtls", ssl = true)
    URL url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyBean.class)
                    .addAsResource(new File("src/test/resources/conf/mtls-jks.conf"), "application.properties")
                    .addAsResource(new File("src/test/resources/conf/server-keystore.jks"), "server-keystore.jks")
                    .addAsResource(new File("src/test/resources/conf/server-truststore.jks"), "server-truststore.jks"));

    @Test
    public void testSslServerWithJKS() {
        RestAssured.given()
                .keyStore(new File("src/test/resources/conf/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/conf/client-truststore.jks"), "password")
                .get(url).then().statusCode(200).body(is("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/mtls").handler(rc -> {
                Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
                Assertions.assertThat(rc.request().isSSL()).isTrue();
                Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();
                try {
                    rc.response().end(rc.request().connection().sslSession().getPeerPrincipal().getName());
                } catch (SSLPeerUnverifiedException cause) {
                    throw new RuntimeException("Failed to obtain peer principal", cause);
                }
            });
        }

    }
}
