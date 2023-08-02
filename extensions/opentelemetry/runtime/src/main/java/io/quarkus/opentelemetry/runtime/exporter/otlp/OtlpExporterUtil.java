package io.quarkus.opentelemetry.runtime.exporter.otlp;

import java.net.URI;
import java.util.Locale;

final class OtlpExporterUtil {

    private OtlpExporterUtil() {
    }

    static int getPort(URI uri) {
        int originalPort = uri.getPort();
        if (originalPort > -1) {
            return originalPort;
        }

        if (isHttps(uri)) {
            return 443;
        }
        return 80;
    }

    static boolean isHttps(URI uri) {
        return "https".equals(uri.getScheme().toLowerCase(Locale.ROOT));
    }
}
