package io.quarkus.opentelemetry.runtime.exporter.otlp;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.opentelemetry.exporter.otlp.internal.OtlpUserAgent;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig;
import io.quarkus.vertx.VertxSupplier;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.Vertx;

public final class OTelExporterUtil {

    private OTelExporterUtil() {
    }

    public static int getPort(URI uri) {
        int originalPort = uri.getPort();
        if (originalPort > -1) {
            return originalPort;
        }

        if (isHttps(uri)) {
            return 443;
        }
        return 80;
    }

    public static boolean isHttps(URI uri) {
        return "https".equals(uri.getScheme().toLowerCase(Locale.ROOT));
    }

    /**
     * The Vert.x instance of the application, created on first use.
     *
     * @throws IllegalStateException when Vert.x is not configured yet
     */
    static Vertx vertx() {
        VertxSupplier vertx = VertxCoreRecorder.getVertx();
        if (vertx == null) {
            throw new IllegalStateException(
                    "Vert.x is not configured yet: the OTLP exporters must be created after Vert.x is configured");
        }
        return vertx.get();
    }

    static boolean determineCompression(OtlpExporterConfig config) {
        if (config.compression().isPresent()) {
            return (config.compression().get() == CompressionType.GZIP);
        }
        return false;
    }

    static Map<String, String> populateTracingExportHttpHeaders(OtlpExporterConfig config) {
        Map<String, String> headersMap = new HashMap<>();
        OtlpUserAgent.addUserAgentHeader(headersMap::put);
        if (config.headers().isPresent()) {
            List<String> headers = config.headers().get();
            if (!headers.isEmpty()) {
                for (String header : headers) {
                    if (header.isEmpty()) {
                        continue;
                    }
                    String[] parts = header.split("=", 2);
                    if (parts.length < 2) {
                        continue;
                    }
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    headersMap.put(key, value);
                }
            }
        }
        return headersMap;
    }
}
