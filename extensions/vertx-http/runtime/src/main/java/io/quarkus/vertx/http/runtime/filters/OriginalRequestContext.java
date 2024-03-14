package io.quarkus.vertx.http.runtime.filters;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

public class OriginalRequestContext {

    public static final String RC_DATA_KEY = "originalRequestContext";

    public static boolean isPresent(RoutingContext rc) {
        OriginalRequestContext originalRequestContext = rc.get(RC_DATA_KEY);
        return originalRequestContext != null;
    }

    public static HttpMethod getMethod(RoutingContext rc) {
        OriginalRequestContext originalRequestContext = rc.get(RC_DATA_KEY);
        if (originalRequestContext == null)
            return null;
        return originalRequestContext.method;
    }

    public static String getUri(RoutingContext rc) {
        OriginalRequestContext originalRequestContext = rc.get(RC_DATA_KEY);
        if (originalRequestContext == null)
            return null;
        return originalRequestContext.uri;
    }

    public static String getPath(RoutingContext rc) {
        OriginalRequestContext originalRequestContext = rc.get(RC_DATA_KEY);
        if (originalRequestContext == null)
            return null;
        return originalRequestContext.path;
    }

    public static String getQuery(RoutingContext rc) {
        OriginalRequestContext originalRequestContext = rc.get(RC_DATA_KEY);
        if (originalRequestContext == null)
            return null;
        return originalRequestContext.query;
    }

    public static List<String> getAllQueryParams(RoutingContext rc, String name) {
        OriginalRequestContext originalRequestContext = rc.get(RC_DATA_KEY);
        if (originalRequestContext == null)
            return null;
        return originalRequestContext.queryParams.getAll(name);
    }

    private final HttpMethod method;
    private final String uri;
    private final String path;
    private final String query;
    private final MultiMap queryParams;

    public OriginalRequestContext(RoutingContext rc) {
        this.method = rc.request().method();
        this.uri = rc.request().uri();
        this.path = rc.request().path();
        this.query = rc.request().query();
        this.queryParams = rc.queryParams();
    }

}
