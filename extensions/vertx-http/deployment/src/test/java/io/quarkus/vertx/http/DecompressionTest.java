package io.quarkus.vertx.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

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
import io.vertx.ext.web.handler.BodyHandler;

public class DecompressionTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.enable-decompression=true\n";

    private static final String LONG_STRING = IntStream.range(0, 1000).mapToObj(i -> "Hello World;")
            .collect(Collectors.joining());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(CompressionTest.BeanRegisteringRouteUsingObserves.class));

    @Test
    public void test() throws Exception {
        var input = LONG_STRING.getBytes(StandardCharsets.UTF_8);
        var bout = new ByteArrayOutputStream(input.length);
        var gzip = new GZIPOutputStream(bout);
        gzip.write(input, 0, input.length);
        gzip.close();
        var compressed = bout.toByteArray();
        bout.close();

        // RestAssured is aware of quarkus.http.root-path
        // If this changes then please modify quarkus-azure-functions-http maven archetype to reflect this
        // in its test classes
        RestAssured.given()
                .header("content-encoding", "gzip")
                .body(compressed)
                .post("/echo").then().statusCode(200)
                .body(Matchers.equalTo(LONG_STRING));

        RestAssured.given()
                .body(LONG_STRING)
                .post("/echo").then().statusCode(200)
                .body(Matchers.equalTo(LONG_STRING));
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {
            router.post("/echo").handler(BodyHandler.create());
            router.post("/echo").handler(rc -> {
                rc.response().end(rc.getBodyAsString());
            });
        }

    }

}
