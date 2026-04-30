package io.quarkus.cyclonedx.endpoint.runtime;

import java.io.IOException;
import java.io.InputStream;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CycloneDxEndpointRecorder {

    @RuntimeInit
    public Handler<RoutingContext> handler(String resourceName, String contentType, boolean compressed) {
        byte[] sbomContent;
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalStateException("Embedded SBOM resource not found: " + resourceName);
            }
            sbomContent = is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read embedded SBOM resource: " + resourceName, e);
        }
        return new SbomHandler(sbomContent, contentType, compressed);
    }

    private static class SbomHandler implements Handler<RoutingContext> {
        private final Buffer content;
        private final String contentType;
        private final boolean compressed;

        SbomHandler(byte[] content, String contentType, boolean compressed) {
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
}
