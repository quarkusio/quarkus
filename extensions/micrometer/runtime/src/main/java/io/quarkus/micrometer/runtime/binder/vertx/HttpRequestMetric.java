package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import io.quarkus.micrometer.runtime.binder.RequestMetricInfo;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

public class HttpRequestMetric extends RequestMetricInfo {
    public static final Pattern VERTX_ROUTE_PARAM = Pattern.compile("^:(.*)$");

    /** Cache of vert.x resolved paths: /item/:id --> /item/{id} */
    final static ConcurrentHashMap<String, String> vertxWebToUriTemplate = new ConcurrentHashMap<>();

    protected HttpServerRequest request;
    protected String initialPath;
    protected String templatePath;

    protected RoutingContext routingContext;

    public HttpRequestMetric(String uri) {
        this.initialPath = uri;
    }

    public HttpRequestMetric(HttpServerRequest request) {
        this.request = request;
        this.initialPath = this.request.path();
    }

    public String getNormalizedUriPath(Map<Pattern, String> matchPatterns, List<Pattern> ignorePatterns) {
        if (isCORSPreflightRequest()) {
            return filterIgnored("/cors-preflight", ignorePatterns);
        }
        return super.getNormalizedUriPath(matchPatterns, ignorePatterns, initialPath);
    }

    public String applyTemplateMatching(String path) {
        String currentRoutePath = getCurrentRoute();

        // JAX-RS or Servlet container filter
        if (templatePath != null) {
            return normalizePath(templatePath);
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

    public HttpServerRequest request() {
        return request;
    }

    public void setTemplatePath(String path) {
        if (this.templatePath == null) {
            this.templatePath = path;
        }
    }

    String getCurrentRoute() {
        return routingContext == null ? null : routingContext.currentRoute().getPath();
    }

    public void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    public static HttpRequestMetric getRequestMetric(RoutingContext context) {
        HttpRequestMetric metric = context.get(VertxHttpServerMetrics.METRICS_CONTEXT);
        return metric;
    }

    @Override
    public String toString() {
        return "HttpRequestMetric [initialPath=" + initialPath + ", currentRoutePath=" + getCurrentRoute()
                + ", templatePath=" + templatePath + ", request=" + request + "]";
    }

    private boolean isCORSPreflightRequest() {
        return request.method() == HttpMethod.OPTIONS
                && request.getHeader("Origin") != null
                && request.getHeader("Access-Control-Request-Method") != null
                && request.getHeader("Access-Control-Request-Headers") != null;
    }
}
