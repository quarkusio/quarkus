package io.quarkus.cyclonedx.endpoint.runtime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CycloneDxEndpointRecorder {

    @RuntimeInit
    public Handler<RoutingContext> handler(String resourceName, String contentType, SbomCompressionMode mode) {
        byte[] sbomContent = readResource(resourceName);
        return switch (mode) {
            // pre-compress once and cache only the compressed bytes
            case ALWAYS -> new FixedSbomHandler(gzip(sbomContent), contentType, true);
            // cache only the uncompressed bytes
            case NEVER -> new FixedSbomHandler(sbomContent, contentType, false);
            // cache only the uncompressed bytes and compress on the fly when the client accepts gzip
            case NEGOTIATE -> new NegotiatingSbomHandler(sbomContent, contentType);
        };
    }

    private static byte[] readResource(String resourceName) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalStateException("Embedded SBOM resource not found: " + resourceName);
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read embedded SBOM resource: " + resourceName, e);
        }
    }

    private static byte[] gzip(byte[] data) {
        var baos = new ByteArrayOutputStream(data.length);
        try (var gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to GZIP-compress the embedded SBOM", e);
        }
        return baos.toByteArray();
    }

    static boolean acceptsGzip(RoutingContext ctx) {
        return acceptsGzip(ctx.request().getHeader(HttpHeaders.ACCEPT_ENCODING));
    }

    /**
     * Whether the given {@code Accept-Encoding} header value accepts the {@code gzip}
     * content coding, honoring an explicit {@code q=0} that disables it.
     */
    static boolean acceptsGzip(String acceptEncoding) {
        if (acceptEncoding == null) {
            return false;
        }
        for (String part : acceptEncoding.toLowerCase(Locale.ROOT).split(",")) {
            String token = part.trim();
            String coding = token;
            double quality = 1.0d;
            int semicolon = token.indexOf(';');
            if (semicolon >= 0) {
                coding = token.substring(0, semicolon).trim();
                int qIndex = token.indexOf("q=", semicolon);
                if (qIndex >= 0) {
                    try {
                        quality = Double.parseDouble(token.substring(qIndex + 2).trim());
                    } catch (NumberFormatException ignored) {
                        // treat an unparsable q-value as the default quality
                    }
                }
            }
            if (quality > 0 && (coding.equals("gzip") || coding.equals("*"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Serves a fixed, pre-computed representation of the SBOM (either always compressed or always raw).
     */
    private static class FixedSbomHandler implements Handler<RoutingContext> {
        private final Buffer content;
        private final String contentType;
        private final boolean compressed;

        FixedSbomHandler(byte[] content, String contentType, boolean compressed) {
            this.content = Buffer.buffer(content);
            this.contentType = contentType;
            this.compressed = compressed;
        }

        @Override
        public void handle(RoutingContext ctx) {
            var response = ctx.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, contentType);
            if (compressed) {
                response.putHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            }
            response.end(content);
        }
    }

    /**
     * Serves the SBOM compressed or raw depending on the request's {@code Accept-Encoding} header.
     * Only the uncompressed bytes are cached; compression is applied on the fly per request.
     */
    private static class NegotiatingSbomHandler implements Handler<RoutingContext> {
        private final byte[] content;
        private final String contentType;

        NegotiatingSbomHandler(byte[] content, String contentType) {
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public void handle(RoutingContext ctx) {
            var response = ctx.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, contentType);
            if (acceptsGzip(ctx)) {
                response.putHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
                response.end(Buffer.buffer(gzip(content)));
            } else {
                response.end(Buffer.buffer(content));
            }
        }
    }
}
