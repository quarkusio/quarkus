package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;

public class CompressionContentSizeThresholdTest {

    private static final String APP_PROPS = ""
            + "quarkus.http.enable-compression=true\n"
            + "quarkus.http.compression-content-size-threshold=1024\n";

    // 1200 bytes — above the 1024-byte threshold
    private static final String LARGE_BODY;
    static {
        StringBuilder sb = new StringBuilder(1200);
        while (sb.length() < 1200) {
            sb.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ");
        }
        LARGE_BODY = sb.substring(0, 1200);
    }

    // 100 bytes — below the 1024-byte threshold
    private static final String SMALL_BODY;
    static {
        StringBuilder sb = new StringBuilder(100);
        while (sb.length() < 100) {
            sb.append("Small. ");
        }
        SMALL_BODY = sb.substring(0, 100);
    }

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(BeanRegisteringRoutes.class));

    @Test
    public void testAboveThresholdIsCompressed() {
        given().get("/above-threshold").then().statusCode(200)
                .header("content-encoding", is("gzip"))
                .header("content-length", Integer::parseInt, lessThan(LARGE_BODY.length()))
                .body(equalTo(LARGE_BODY));
    }

    @Test
    public void testBelowThresholdIsNotCompressed() {
        given().get("/below-threshold").then().statusCode(200)
                .header("content-encoding", is(nullValue()))
                .header("content-length", Integer::parseInt, equalTo(SMALL_BODY.length()))
                .body(equalTo(SMALL_BODY));
    }

    @ApplicationScoped
    static class BeanRegisteringRoutes {

        public void register(@Observes Router router) {
            router.route("/above-threshold").handler(rc -> {
                rc.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
                rc.response().end(LARGE_BODY);
            });
            router.route("/below-threshold").handler(rc -> {
                rc.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
                rc.response().end(SMALL_BODY);
            });
        }
    }
}
