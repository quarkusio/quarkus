package io.quarkus.micrometer.runtime.binder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.spi.observability.HttpResponse;
import io.vertx.ext.web.RoutingContext;

public class HttpRequestMetric {
    static final Logger log = Logger.getLogger(HttpRequestMetric.class);

    public static final String HTTP_REQUEST_PATH = "HTTP_REQUEST_PATH";
    public static final String HTTP_REQUEST_PATH_MATCHED = "HTTP_REQUEST_MATCHED_PATH";
    public static final Pattern VERTX_ROUTE_PARAM = Pattern.compile("^:(.*)$");

    /** Cache of vert.x resolved paths: /item/:id --> /item/{id} */
    final static ConcurrentHashMap<String, String> templatePath = new ConcurrentHashMap<>();

    volatile RoutingContext routingContext;

    /** Do not measure requests until/unless a uri path is set */
    final boolean measure;

    /** URI path used as a tag value for non-error requests */
    final String path;

    /** True IFF the path was revised by a matcher expression */
    final boolean pathMatched;

    /** Store the sample used to measure the request */
    Timer.Sample sample;

    /**
     * Store the tags associated with the request (change Micrometer 1.6.0).
     * Default is empty, value assigned @ requestBegin
     */
    Tags tags = Tags.empty();

    /** Response associated with the request (Vert.x 4.0) */
    HttpResponse response;

    public Timer.Sample getSample() {
        return sample;
    }

    public void setSample(Timer.Sample sample) {
        this.sample = sample;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    /**
     * Extract the path out of the uri. Return null if the path should be
     * ignored.
     */
    public HttpRequestMetric(Map<Pattern, String> matchPattern, List<Pattern> ignorePatterns,
            String uri) {
        if (uri == null) {
            this.measure = false;
            this.pathMatched = false;
            this.path = null;
            return;
        }

        boolean matched = false;
        String workingPath = extractPath(uri);
        String finalPath = workingPath;
        if ("/".equals(workingPath) || workingPath.isEmpty()) {
            finalPath = "/";
        } else {
            // Label value consistency: result should begin with a '/' and should not end with one
            workingPath = HttpMetricsCommon.MULTIPLE_SLASH_PATTERN.matcher('/' + workingPath).replaceAll("/");
            workingPath = HttpMetricsCommon.TRAILING_SLASH_PATTERN.matcher(workingPath).replaceAll("");
            if (workingPath.isEmpty()) {
                finalPath = "/";
            } else {
                finalPath = workingPath;
                // test path against configured patterns (whole path)
                for (Map.Entry<Pattern, String> mp : matchPattern.entrySet()) {
                    if (mp.getKey().matcher(workingPath).matches()) {
                        finalPath = mp.getValue();
                        matched = true;
                        break;
                    }
                }
            }
        }
        this.path = finalPath;
        this.pathMatched = matched;

        // Compare path against "ignore this path" patterns
        for (Pattern p : ignorePatterns) {
            if (p.matcher(this.path).matches()) {
                log.debugf("Path %s ignored; matches pattern %s", uri, p.pattern());
                this.measure = false;
                return;
            }
        }
        this.measure = true;
    }

    public Timer.Sample getSample() {
        return sample;
    }

    public void setSample(Timer.Sample sample) {
        this.sample = sample;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public String getPath() {
        return path;
    }

    public boolean isMeasure() {
        return measure;
    }

    public boolean isPathMatched() {
        return pathMatched;
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

    public String getHttpRequestPath() {
        // Vertx binder configuration, see VertxMetricsTags
        if (pathMatched) {
            return path;
        }
        if (routingContext != null) {
            // JAX-RS or Servlet container filter
            String rcPath = routingContext.get(HTTP_REQUEST_PATH);
            if (rcPath != null) {
                return rcPath;
            }
            // vertx-web or reactive route
            String matchedPath = routingContext.currentRoute().getPath();
            if (matchedPath != null) {
                if (matchedPath.contains(":")) {
                    // Convert /item/:id to /item/{id} and save it for next time
                    matchedPath = templatePath.computeIfAbsent(matchedPath, k -> {
                        String segments[] = k.split("/");
                        for (int i = 0; i < segments.length; i++) {
                            segments[i] = VERTX_ROUTE_PARAM.matcher(segments[i]).replaceAll("{$1}");
                        }
                        return String.join("/", segments);
                    });
                }
                return matchedPath;
            }
        }
        return path;
    }

    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    public void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    @Override
    public String toString() {
        return "HttpRequestMetric{path=" + path
                + ",pathMatched=" + pathMatched
                + ",measure=" + measure
                + ",tags=" + tags
                + '}';
    }
}
