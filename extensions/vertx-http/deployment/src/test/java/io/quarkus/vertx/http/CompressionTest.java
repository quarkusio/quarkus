package io.quarkus.vertx.http;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;

public class CompressionTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.enable-compression=true\n";

    static String longString;
    static {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            sb.append("Hello World;");
        }
        longString = sb.toString();
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(BeanRegisteringRouteUsingObserves.class));

    @Test
    public void test() throws Exception {
        // RestAssured is aware of quarkus.http.root-path
        // If this changes then please modify quarkus-azure-functions-http maven archetype to reflect this
        // in its test classes
        RestAssured.given().get("/compress").then().statusCode(200)
                .header("content-encoding", "gzip")
                .header("content-length", Matchers.not(Matchers.equalTo(Integer.toString(longString.length()))))
                .body(Matchers.equalTo(longString));

        RestAssured.given().get("/nocompress").then().statusCode(200)
                .header("content-encoding", is(nullValue()))
                .header("content-length", Matchers.equalTo(Integer.toString(longString.length())))
                .body(Matchers.equalTo(longString));
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {

            router.route("/compress").handler(rc -> {
                // The content-encoding header must be removed
                rc.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
                rc.response().end(longString);
            });
            router.route("/nocompress").handler(rc -> {
                // This header is set by default
                // rc.response().headers().set("content-encoding", "identity");
                rc.response().end(longString);
            });
        }

    }

}
