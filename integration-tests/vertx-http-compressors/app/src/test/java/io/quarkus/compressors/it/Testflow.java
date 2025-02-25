package io.quarkus.compressors.it;

import static io.netty.handler.codec.compression.ZlibCodecFactory.newZlibDecoder;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.quarkus.arc.ComponentsProvider.LOG;
import static io.quarkus.compressors.it.CompressedResource.TEXT;
import static java.lang.Integer.parseInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.BrotliDecoder;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class Testflow {

    // Depending on the defaults of the compression algorithm,
    // the compressed content length may vary slightly between
    // Vert.x/Netty versions over time.
    public static final int COMPRESSION_TOLERANCE_PERCENT = 2;

    static {
        // Our test code does compression
        Brotli4jLoader.ensureAvailability();
    }

    /**
     * This test logic is shared by both "all" module and "some" module. See their RESTEndpointsTest classes.
     *
     * @param endpoint
     * @param acceptEncoding
     * @param contentEncoding
     * @param contentLength
     */
    public static void runCompressorsTest(String endpoint, String acceptEncoding, String contentEncoding,
            String contentLength) {
        LOG.infof("Endpoint %s; Accept-Encoding: %s; Content-Encoding: %s; Content-Length: %s",
                endpoint, acceptEncoding, contentEncoding, contentLength);
        // RestAssured
        // -----------
        // Why not use RestAssured? Because it doesn't let you configure Accept-Encoding easily
        // and when it comes to Brotli, not at all.
        // No, given().header("Accept-Encoding", acceptEncoding) doesn't cut it.
        final WebClient client = WebClient.create(Vertx.vertx(), new WebClientOptions()
                .setLogActivity(true)
                .setFollowRedirects(true)
                // Vert.x Web Client
                // -----------------
                // Why not use the client's built-in decompression support?
                // Why you do decompression manually here?
                // Because it then removes the original content-encoding header,
                // and it fakes in a transfer-encoding header the server has never sent. Sad.
                // RestAssured didn't let us configure Accept-Encoding easily, but at least
                // it didn't mess with the response headers.
                .setDecompressionSupported(false));
        final CompletableFuture<HttpResponse<Buffer>> future = new CompletableFuture<>();
        client.requestAbs(HttpMethod.GET, endpoint)
                .putHeader(HttpHeaders.ACCEPT_ENCODING.toString(), acceptEncoding)
                .putHeader(HttpHeaders.ACCEPT.toString(), "*/*")
                .putHeader(HttpHeaders.USER_AGENT.toString(), "Tester")
                .send(ar -> {
                    if (ar.succeeded()) {
                        future.complete(ar.result());
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        try {
            final HttpResponse<Buffer> response = future.get();
            final String actualEncoding = response.headers().get("content-encoding");

            assertEquals(OK.code(), response.statusCode(),
                    "Http status must be OK.");
            assertEquals(contentEncoding, actualEncoding,
                    "Unexpected compressor selected.");

            final int receivedLength = parseInt(response.headers().get("content-length"));
            final int expectedLength = parseInt(contentLength);

            if (contentEncoding == null) {
                assertEquals(expectedLength, receivedLength,
                        "No compression was expected, so the content-length must match exactly.");
            } else {
                final int expectedLengthWithTolerance = expectedLength + (expectedLength / 100 * COMPRESSION_TOLERANCE_PERCENT);
                assertTrue(receivedLength <= expectedLengthWithTolerance,
                        "Compression apparently failed: receivedLength: " + receivedLength +
                                " was supposed to be less or equal to expectedLength: " +
                                expectedLength + " plus " + COMPRESSION_TOLERANCE_PERCENT + "% tolerance, i.e. "
                                + expectedLengthWithTolerance + ".");
            }
            assertEquals(TEXT, decompress(actualEncoding, response.body().getBytes()), "Unexpected body text.");
        } catch (InterruptedException | ExecutionException e) {
            fail(e);
        }
    }

    public static void runDecompressorsTest(String endpoint, String acceptEncoding, String contentEncoding,
            String method) {
        LOG.infof("Endpoint %s; Accept-Encoding: %s; Content-Encoding: %s; Method: %s",
                endpoint, acceptEncoding, contentEncoding, method);
        final WebClient client = WebClient.create(Vertx.vertx(), new WebClientOptions()
                .setLogActivity(true)
                .setFollowRedirects(true)
                .setDecompressionSupported(false));
        final CompletableFuture<HttpResponse<Buffer>> future = new CompletableFuture<>();
        client.postAbs(endpoint)
                .putHeader(HttpHeaders.CONTENT_ENCODING.toString(), contentEncoding)
                .putHeader(HttpHeaders.ACCEPT.toString(), "*/*")
                .putHeader(HttpHeaders.USER_AGENT.toString(), "Tester")
                .sendBuffer(compress(contentEncoding, TEXT), ar -> {
                    if (ar.succeeded()) {
                        future.complete(ar.result());
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        try {
            final HttpResponse<Buffer> response = future.get();
            final String actualEncoding = response.headers().get("content-encoding");
            final String body = decompress(actualEncoding, response.body().getBytes());
            assertEquals(OK.code(), response.statusCode(), "Http status must be OK.");
            assertEquals(TEXT, body, "Unexpected body text.");
        } catch (InterruptedException | ExecutionException e) {
            fail(e);
        }
    }

    public static Buffer compress(String algorithm, String payload) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        if ("gzip".equalsIgnoreCase(algorithm)) {
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                gzipStream.write(payload.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Gzip compression failed", e);
            }
            return Buffer.buffer(byteStream.toByteArray());
        } else if ("br".equalsIgnoreCase(algorithm)) {
            try (BrotliOutputStream brotliStream = new BrotliOutputStream(byteStream)) {
                brotliStream.write(payload.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Brotli compression failed", e);
            }
            return Buffer.buffer(byteStream.toByteArray());
        } else if ("deflate".equalsIgnoreCase(algorithm)) {
            try (DeflaterOutputStream deflateStream = new DeflaterOutputStream(byteStream)) {
                deflateStream.write(payload.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Deflate compression failed", e);
            }
            return Buffer.buffer(byteStream.toByteArray());
        } else {
            throw new IllegalArgumentException("Unsupported encoding: " + algorithm);
        }
    }

    public static String decompress(String algorithm, byte[] payload) {
        if (algorithm != null && !"identity".equalsIgnoreCase(algorithm)) {
            final EmbeddedChannel channel;
            if ("gzip".equalsIgnoreCase(algorithm)) {
                channel = new EmbeddedChannel(newZlibDecoder(ZlibWrapper.GZIP));
            } else if ("deflate".equalsIgnoreCase(algorithm)) {
                channel = new EmbeddedChannel(newZlibDecoder(ZlibWrapper.ZLIB));
            } else if ("br".equalsIgnoreCase(algorithm)) {
                channel = new EmbeddedChannel(new BrotliDecoder());
            } else {
                throw new RuntimeException("Unexpected compression used by server: " + algorithm);
            }
            channel.writeInbound(Unpooled.copiedBuffer(payload));
            channel.finish();
            final ByteBuf decompressed = channel.readInbound();
            return decompressed.readCharSequence(decompressed.readableBytes(), StandardCharsets.UTF_8).toString();
        } else {
            return new String(payload, StandardCharsets.UTF_8);
        }
    }
}
