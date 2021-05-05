package io.quarkus.resteasy.reactive;

import java.util.function.Supplier;

import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                            .addClasses(TestCompression.class);
                }
            });

    @Test
    public void testServerCompression() throws Exception {

        RestAssured.given().get("/test/compression").then().statusCode(200)
                .header("content-encoding", "gzip")
                .header("content-length", Matchers.not(Matchers.equalTo(Integer.toString(longString.length()))))
                .body(Matchers.equalTo(longString));

        RestAssured.given().get("/test/nocompression").then().statusCode(200)
                .header("content-encoding", "identity")
                .header("content-length", Matchers.equalTo(Integer.toString(longString.length())))
                .body(Matchers.equalTo(longString));
    }

    //        @ApplicationScoped
    //        static class BeanRegisteringRouteUsingObserves {
    //
    //            public void register(@Observes Router router) {
    //
    //                router.route("/compression").handler(rc -> {
    //                    rc.response().end(longString);
    //                });
    //                router.route("/nocompression").handler(rc -> {
    //                    rc.response().headers().set("content-encoding", "identity");
    //                    rc.response().end(longString);
    //                });
    //            }
    //
    //        }

    @Path("/test")
    public static class TestCompression {

        //TODO - complete logic in method
        @Path("/compression")
        @GET
        public void registerCompression(@Observes Router router) {
            router.route("/test/compression").handler(routingContext -> {
                routingContext.response().end(longString);
            });
        }

        @Path("/nocompression")
        @GET
        public void registerNoCompression(@Observes Router router) {

        }
    }

}
