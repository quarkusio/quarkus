package io.quarkus.resteasy.reactive;

import java.util.function.Supplier;

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

        @Path("/compression")
        @GET
        public String registerCompression() {
            return longString;
        }

        @Path("/nocompression")
        @GET
        public String registerNoCompression() {
            return longString;
        }
    }
}
