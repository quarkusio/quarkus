package io.quarkus.opentelemetry.runtime.exporter.otlp;

import java.net.URI;
import java.util.Locale;

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
}
