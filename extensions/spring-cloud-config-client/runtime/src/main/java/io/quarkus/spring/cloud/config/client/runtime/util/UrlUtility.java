package io.quarkus.spring.cloud.config.client.runtime.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlUtility {

    public static int getPort(URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : (isHttps(uri) ? 443 : 80);
    }

    public static boolean isHttps(URI uri) {
        return uri.getScheme().contains("https");
    }

    public static URI toURI(String url) throws URISyntaxException {
        return new URI(sanitize(url));
    }

    public static String sanitize(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

}
