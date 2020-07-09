package io.quarkus.vertx.http.logging;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

class CustomFormatRequestLoggingHandlerTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.request-logger.enabled=true\n" +
            "quarkus.http.request-logger.format=long";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(MyBean.class));

    @Test
    void handle() {
        RestAssured.given().get("/api").then().statusCode(200);
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/api").handler(rc -> rc.response().end());
        }

    }

}
