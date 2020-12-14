package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.http.Outcome;
import io.quarkus.micrometer.runtime.binder.HttpTags;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class VertxMetricsTags {
    private static final Logger log = Logger.getLogger(VertxMetricsTags.class);

    /**
     * Creates a {@code method} tag based on the {@link HttpServerRequest#method()}
     * method} of the given {@code request}.
     *
     * @param method the request method
     * @return the method tag whose value is a capitalized method (e.g. GET).
     */
    public static Tag method(HttpMethod method) {
        return (method != null) ? Tag.of("method", method.toString()) : HttpTags.METHOD_UNKNOWN;
    }

    /**
     * Creates an {@code outcome} {@code Tag} derived from the given {@code response}.
     *
     * @param response the response
     * @return the outcome tag
     */
    public static Tag outcome(HttpServerResponse response) {
        if (response != null) {
            return Outcome.forStatus(response.getStatusCode()).asTag();
        }
        return Outcome.UNKNOWN.asTag();
    }

    /**
     * Creates an {@code outcome} {@code Tag} derived from the given {@code response}.
     *
     * @param response the response
     * @return the outcome tag
     */
    public static Tag outcome(HttpClientResponse response) {
        if (response != null) {
            return Outcome.forStatus(response.statusCode()).asTag();
        }
        return Outcome.UNKNOWN.asTag();
    }

    /**
     * Extract the path out of the uri. Return null if the path should be
     * ignored.
     */
    static void parseUriPath(RequestMetric requestMetric, Map<Pattern, String> matchPattern, List<Pattern> ignorePatterns,
            String uri) {
        if (uri == null) {
            return;
        }

        String path = "/" + extractPath(uri);
        path = HttpTags.MULTIPLE_SLASH_PATTERN.matcher(path).replaceAll("/");
        path = HttpTags.TRAILING_SLASH_PATTERN.matcher(path).replaceAll("");

        if (path.isEmpty()) {
            path = "/";
        }
        requestMetric.path = path;
        for (Map.Entry<Pattern, String> mp : matchPattern.entrySet()) {
            requestMetric.path = mp.getKey().matcher(requestMetric.path).replaceAll(mp.getValue());
        }
        requestMetric.pathMatched = !path.equals(requestMetric.path);

        // Compare path against "ignore this path" patterns
        for (Pattern p : ignorePatterns) {
            if (p.matcher(path).matches()) {
                log.debugf("Path %s ignored; matches pattern %s", uri, p.pattern());
                return;
            }
        }
        requestMetric.measure = true;
    }

    private static String extractPath(String uri) {
        if (uri.isEmpty()) {
            return uri;
        }
        int i;
        if (uri.charAt(0) == '/') {
            i = 0;
        } else {
            i = uri.indexOf("://");
            if (i == -1) {
                i = 0;
            } else {
                i = uri.indexOf('/', i + 3);
                if (i == -1) {
                    // contains no /
                    return "/";
                }
            }
        }

        int queryStart = uri.indexOf('?', i);
        if (queryStart == -1) {
            queryStart = uri.length();
        }
        return uri.substring(i, queryStart);
    }
}
