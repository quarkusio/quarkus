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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.BrotliEncoder;
import io.netty.handler.codec.compression.JdkZlibEncoder;
import io.netty.handler.codec.compression.SnappyFrameEncoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.compression.Zstd;
import io.netty.handler.codec.compression.ZstdEncoder;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class DecompressionTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.enable-decompression=true\n";

    private static final String LONG_STRING = IntStream.range(0, 1000).mapToObj(i -> "Hello World;")
            .collect(Collectors.joining());

    private static final byte[] INPUT = LONG_STRING.getBytes(StandardCharsets.UTF_8);

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(EchoRoute.class));

    @Test
    public void testUncompressed() {
        // RestAssured is aware of quarkus.http.root-path
        // If this changes then please modify quarkus-azure-functions-http maven archetype to reflect this
        // in its test classes
        RestAssured.given()
                .body(LONG_STRING)
                .post("/echo").then().statusCode(200)
                .body(Matchers.equalTo(LONG_STRING));
    }

    @Test
    public void testGzipRoundTrip() throws Exception {
        assertRoundTrip("gzip", gzipBytes(INPUT));
    }

    @Test
    public void testXGzipRoundTrip() throws Exception {
        assertRoundTrip("x-gzip", gzipBytes(INPUT));
    }

    @Test
    public void testDeflateRoundTrip() {
        assertRoundTrip("deflate", encodeWith(new JdkZlibEncoder(ZlibWrapper.ZLIB), INPUT));
    }

    @Test
    public void testXDeflateRoundTrip() {
        assertRoundTrip("x-deflate", encodeWith(new JdkZlibEncoder(ZlibWrapper.ZLIB), INPUT));
    }

    @Test
    public void testSnappyFramedRoundTrip() {
        assertRoundTrip("snappy", encodeWith(new SnappyFrameEncoder(), INPUT));
    }

    @Test
    public void testBrotliRoundTrip() {
        Assumptions.assumeTrue(Brotli.isAvailable(), "Brotli is not available on this platform");
        assertRoundTrip("br", encodeWith(new BrotliEncoder(), INPUT));
    }

    @Test
    public void testZstdRoundTrip() {
        Assumptions.assumeTrue(Zstd.isAvailable(), "Zstd is not available on this platform");
        assertRoundTrip("zstd", encodeWith(new ZstdEncoder(), INPUT));
    }

    private static void assertRoundTrip(String contentEncoding, byte[] compressed) {
        RestAssured.given()
                .header("content-encoding", contentEncoding)
                .body(compressed)
                .post("/echo").then().statusCode(200)
                .body(Matchers.equalTo(LONG_STRING));
    }

    private static byte[] gzipBytes(byte[] input) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(input.length);
        try (GZIPOutputStream gzip = new GZIPOutputStream(bout)) {
            gzip.write(input);
        }
        return bout.toByteArray();
    }

    private static byte[] encodeWith(ChannelHandler encoder, byte[] input) {
        EmbeddedChannel channel = new EmbeddedChannel(encoder);
        try {
            channel.writeOutbound(Unpooled.wrappedBuffer(input));
            try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
                ByteBuf part;
                while ((part = channel.readOutbound()) != null) {
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
                return bout.toByteArray();
            }
        } finally {
            channel.finishAndReleaseAll();
        }
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
