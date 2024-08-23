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

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Router;

public class CompressionTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.enable-compression=true\n";

    public static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
            "incididunt ut labore et " +
            "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip " +
            "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
            "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt " +
            "mollit anim id est laborum." +
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et " +
            "dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip " +
            "ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
            "fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt " +
            "mollit anim id est laborum.";

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
        given().get("/compress").then().statusCode(200)
                .header("content-encoding", is("gzip"))
                .header("content-length", Integer::parseInt, lessThan(TEXT.length()))
                .body(equalTo(TEXT));

        // Why don't you just given().header("Accept-Encoding", "deflate")?
        // Because RestAssured silently ignores that and sends gzip anyway,
        // search RestAssured GitHub for Accept-Encoding and decoder config.
        given().config(RestAssured.config
                .decoderConfig(DecoderConfig.decoderConfig().with().contentDecoders(DecoderConfig.ContentDecoder.DEFLATE)))
                .get("/compress").then().statusCode(200)
                .header("content-encoding", is("deflate"))
                .header("content-length", Integer::parseInt, lessThan(TEXT.length()))
                .body(equalTo(TEXT));

        given().get("/nocompress").then().statusCode(200)
                .header("content-encoding", is(nullValue()))
                .header("content-length", Integer::parseInt, equalTo(TEXT.length()))
                .body(equalTo(TEXT));
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {

            router.route("/compress").handler(rc -> {
                // The content-encoding header must be removed
                rc.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
                rc.response().end(TEXT);
            });
            router.route("/nocompress").handler(rc -> {
                // This header is set by default
                // rc.response().headers().set("content-encoding", "identity");
                rc.response().end(TEXT);
            });
        }

    }

}
