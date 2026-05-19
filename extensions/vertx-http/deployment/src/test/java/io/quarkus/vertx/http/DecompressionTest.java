package io.quarkus.vertx.http;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class DecompressionTest {

    private static final String LONG_STRING = IntStream.range(0, 1000).mapToObj(i -> "Hello World;")
            .collect(Collectors.joining());

    private static final String APP_PROPS = "quarkus.http.enable-decompression=true\n";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(EchoRoute.class));

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

    @Test
    public void testSnappyFramedRoundTrip() {
        byte[] input = LONG_STRING.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = snappyFramed(input);

        RestAssured.given()
                .header("content-encoding", "snappy")
                .body(compressed)
                .post("/echo").then().statusCode(200)
                .body(Matchers.equalTo(LONG_STRING));
    }

    private static byte[] snappyFramed(byte[] input) {
        EmbeddedChannel encoder = new EmbeddedChannel(new SnappyFrameEncoder());
        encoder.writeOutbound(Unpooled.wrappedBuffer(input));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteBuf part;
        while ((part = encoder.readOutbound()) != null) {
            try {
                int n = part.readableBytes();
                if (n > 0) {
                    byte[] chunk = new byte[n];
                    part.getBytes(part.readerIndex(), chunk);
                    part.skipBytes(n);
                    bout.write(chunk, 0, chunk.length);
                }
            } finally {
                part.release();
            }
        }
        encoder.finishAndReleaseAll();
        return bout.toByteArray();
    }

    @ApplicationScoped
    static class EchoRoute {

        public void register(@Observes Router router) {
            router.post("/echo").handler(BodyHandler.create());
            router.post("/echo").handler(rc -> {
                Buffer body = rc.body().buffer();
                rc.response().end(body != null ? body : Buffer.buffer());
            });
        }
    }

}
