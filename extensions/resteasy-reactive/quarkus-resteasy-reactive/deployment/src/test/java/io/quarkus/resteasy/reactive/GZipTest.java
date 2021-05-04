package io.quarkus.resteasy.reactive;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class GZipTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.enable-compression=true\n";

    static String longString;
    static {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            sb.append("Hello RESTEasy Reactive;");
        }
        longString = sb.toString();
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(BeanRegisteringRouteUsingObserves.class));

    @Test
    public void testServerCompression() throws Exception {

        RestAssured.given().get("/compression").then().statusCode(200)
                .header("content-encoding", "gzip")
                .header("content-length", Matchers.not(Matchers.equalTo(Integer.toString(longString.length()))))
                .body(Matchers.equalTo(longString));

        RestAssured.given().get("/nocompression").then().statusCode(200)
                .header("content-encoding", "identity")
                .header("content-length", Matchers.equalTo(Integer.toString(longString.length())))
                .body(Matchers.equalTo(longString));
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {

            router.route("/compression").handler(rc -> {
                rc.response().end(longString);
            });
            router.route("/nocompression").handler(rc -> {
                rc.response().headers().set("content-encoding", "identity");
                rc.response().end(longString);
            });
        }

    }
}
