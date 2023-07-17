package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;

import io.quarkus.micrometer.runtime.binder.RequestMetricInfo;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.ext.web.RoutingContext;

public class HttpRequestMetric extends RequestMetricInfo {
    public static final Pattern VERTX_ROUTE_PARAM = Pattern.compile("^:(.*)$");

    /** Cache of vert.x resolved paths: /item/:id --> /item/{id} */
    final static ConcurrentHashMap<String, String> vertxWebToUriTemplate = new ConcurrentHashMap<>();

    protected HttpServerRequestInternal request;
    protected String initialPath;
    protected String templatePath;
    protected String currentRoutePath;
    private final LongAdder activeRequests;

    private boolean requestActive = false;

    public HttpRequestMetric(String uri, LongAdder activeRequests) {
        this.initialPath = uri;
        this.activeRequests = activeRequests;
    }

    public HttpRequestMetric(HttpRequest request, LongAdder activeRequests) {
        this.request = (HttpServerRequestInternal) request;
        this.initialPath = this.request.path();
        this.activeRequests = activeRequests;
    }

    public String getNormalizedUriPath(Map<Pattern, String> matchPatterns, List<Pattern> ignorePatterns) {
        if (isCORSPreflightRequest()) {
            return filterIgnored("/cors-preflight", ignorePatterns);
        }
        return super.getNormalizedUriPath(matchPatterns, ignorePatterns, initialPath);
    }

    public String applyTemplateMatching(String path) {
        // JAX-RS: UrlPathTemplate set in the
        String urlTemplatePath = getUrlTemplatePath();
        if (urlTemplatePath != null) {
            return normalizePath(urlTemplatePath);
        }

        // vertx-web or reactive route: is it templated?
        if (currentRoutePath != null && currentRoutePath.contains(":")) {
            // Convert /item/:id to /item/{id} and save it for next time
            return vertxWebToUriTemplate.computeIfAbsent(currentRoutePath, k -> {
                String segments[] = k.split("/");
                for (int i = 0; i < segments.length; i++) {
                    segments[i] = VERTX_ROUTE_PARAM.matcher(segments[i]).replaceAll("{$1}");
                }
                return normalizePath(String.join("/", segments));
            });
        }

        return path;
    }

    public HttpServerRequestInternal request() {
        return request;
    }

    public void requestStarted() {
        if (!requestActive) {
            requestActive = true;
            activeRequests.increment();
        }
    }

    public void requestEnded() {
        if (requestActive) {
            requestActive = false;
            activeRequests.decrement();
        }
    }

    public void setTemplatePath(String path) {
        if (this.templatePath == null) {
            this.templatePath = path;
        }
    }

    public void appendCurrentRoutePath(String path) {
        if (path != null && !path.isEmpty()) {
            this.currentRoutePath = path;
        }
    }

    public static HttpRequestMetric getRequestMetric(RoutingContext context) {
        HttpServerRequestInternal internalRequest = (HttpServerRequestInternal) context.request();
        return (HttpRequestMetric) internalRequest.metric();
    }

    String getUrlTemplatePath() {
        String urlTemplatePath = null;
        if (request != null) {
            urlTemplatePath = request.context().getLocal("UrlPathTemplate");
        }
        // Fall back to Servlet container filter set templatePath if a path was not set in the request context
        return (urlTemplatePath == null ? templatePath : urlTemplatePath);
    }

    @Override
    public String toString() {
        return "HttpRequestMetric [initialPath=" + initialPath + ", currentRoutePath=" + currentRoutePath
                + ", templatePath=" + templatePath + ", request=" + request + "]";
    }

    private boolean isCORSPreflightRequest() {
        return request.method() == HttpMethod.OPTIONS
                && request.getHeader("Origin") != null
                && request.getHeader("Access-Control-Request-Method") != null
                && request.getHeader("Access-Control-Request-Headers") != null;
    }
}
