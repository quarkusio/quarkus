package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;

import io.quarkus.micrometer.runtime.binder.RequestMetricInfo;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.ext.web.RoutingContext;

public class HttpRequestMetric extends RequestMetricInfo {
    public static final Pattern VERTX_ROUTE_PARAM = Pattern.compile("^:(.*)$");

    /** Cache of vert.x resolved paths: /item/:id --> /item/{id} */
    final static ConcurrentHashMap<String, String> vertxWebToUriTemplate = new ConcurrentHashMap<>();

    private HttpServerRequestInternal request;
    private HttpRequest httpRequest;
    private String initialPath;
    private String templatePath;
    private String currentRoutePath;
    private String normalizedPath;
    private boolean normalizedPathComputed = false;
    private final LongAdder activeRequests;
    private Context executionContext;

    private boolean requestActive = false;

    public HttpRequestMetric(String uri, LongAdder activeRequests) {
        this.initialPath = uri;
        this.activeRequests = activeRequests;
    }

    public HttpRequestMetric(HttpRequest request, LongAdder activeRequests) {
        this.httpRequest = request;
        if (request instanceof HttpServerRequestInternal internal) {
            this.request = internal;
        }
        this.initialPath = request.uri();
        this.activeRequests = activeRequests;
    }

    public String getNormalizedUriPath(Map<Pattern, String> matchPatterns, List<Pattern> ignorePatterns) {
        if (normalizedPathComputed) {
            return normalizedPath;
        }
        if (isCORSPreflightRequest()) {
            normalizedPath = filterIgnored("/cors-preflight", ignorePatterns);
        } else {
            normalizedPath = super.getNormalizedUriPath(matchPatterns, ignorePatterns, initialPath);
        }
        normalizedPathComputed = true;
        return normalizedPath;
    }

    public String getInitialPath() {
        return initialPath;
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

    public HttpRequest httpRequest() {
        return httpRequest;
    }

    public void setExecutionContext(Context ctx) {
        this.executionContext = ctx;
    }

    public Context getExecutionContext() {
        return executionContext;
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

    public String getRoute() {
        if (currentRoutePath == null || currentRoutePath.isEmpty()) {
            return "";
        }
        return currentRoutePath;
    }

    public static HttpRequestMetric getRequestMetric(RoutingContext context) {
        HttpServerRequestInternal internalRequest = (HttpServerRequestInternal) context.request();
        return (HttpRequestMetric) internalRequest.metric();
    }

    String getUrlTemplatePath() {
        String urlTemplatePath = null;
        if (executionContext != null && VertxContext.isDuplicatedContext(executionContext)) {
            urlTemplatePath = (String) VertxContext.localContextData(executionContext).get("UrlPathTemplate");
        }
        if (urlTemplatePath == null && request != null) {
            Context ctx = request.context();
            if (VertxContext.isDuplicatedContext(ctx)) {
                urlTemplatePath = (String) VertxContext.localContextData(ctx).get("UrlPathTemplate");
            }
        }
        return (urlTemplatePath == null ? templatePath : urlTemplatePath);
    }

    @Override
    public String toString() {
        return "HttpRequestMetric [initialPath=" + initialPath + ", currentRoutePath=" + currentRoutePath
                + ", templatePath=" + templatePath + ", request=" + request + "]";
    }

    private boolean isCORSPreflightRequest() {
        HttpRequest req = httpRequest();
        return req.method() == HttpMethod.OPTIONS
                && req.headers().get("Origin") != null
                && req.headers().get("Access-Control-Request-Method") != null
                && req.headers().get("Access-Control-Request-Headers") != null;
    }
}
