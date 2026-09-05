package io.quarkus.vertx.http.runtime.filters.accesslog;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.UserContext;

/**
 * Minimal {@link RoutingContext} for invalid requests that never reach the Vert.x Web router.
 * <p>
 * Enough for {@link io.quarkus.vertx.http.runtime.attribute.ExchangeAttribute} formatting:
 * delegates {@link #request()} / {@link #response()}, and returns empty/null for router-only state.
 * Missing values are turned into {@code -} by {@link io.quarkus.vertx.http.runtime.attribute.SubstituteEmptyWrapper}.
 */
final class InvalidRequestRoutingContext implements RoutingContext {

    private final HttpServerRequest request;
    private Map<String, Object> data;

    InvalidRequestRoutingContext(HttpServerRequest request) {
        this.request = request;
    }

    @Override
    public HttpServerRequest request() {
        return request;
    }

    @Override
    public HttpServerResponse response() {
        return request.response();
    }

    @Override
    public void next() {
    }

    @Override
    public void fail(int statusCode) {
    }

    @Override
    public void fail(Throwable throwable) {
    }

    @Override
    public void fail(int statusCode, Throwable throwable) {
    }

    @Override
    public RoutingContext put(String key, Object obj) {
        getData().put(key, obj);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        if (data == null) {
            return null;
        }
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key, T defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        T value = (T) data.get(key);
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T remove(String key) {
        if (data == null) {
            return null;
        }
        return (T) data.remove(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Map<String, T> data() {
        return (Map<String, T>) getData();
    }

    private Map<String, Object> getData() {
        if (data == null) {
            data = new HashMap<>();
        }
        return data;
    }

    @Override
    public Vertx vertx() {
        return null;
    }

    @Override
    public String mountPoint() {
        return null;
    }

    @Override
    public Route currentRoute() {
        return null;
    }

    @Override
    public String normalizedPath() {
        String path = request.path();
        return path != null ? path : "/";
    }

    @Override
    public RequestBody body() {
        return null;
    }

    @Override
    public List<FileUpload> fileUploads() {
        return Collections.emptyList();
    }

    @Override
    public void cancelAndCleanupFileUploads() {
    }

    @Override
    public Session session() {
        return null;
    }

    @Override
    public boolean isSessionAccessed() {
        return false;
    }

    @Override
    public UserContext userContext() {
        return null;
    }

    @Override
    public User user() {
        // Override default RoutingContext.user() which would NPE on a null userContext().
        return null;
    }

    @Override
    public Throwable failure() {
        return null;
    }

    @Override
    public int statusCode() {
        return request.response().getStatusCode();
    }

    @Override
    public String getAcceptableContentType() {
        return null;
    }

    @Override
    public ParsedHeaderValues parsedHeaders() {
        return null;
    }

    @Override
    public int addHeadersEndHandler(Handler<Void> handler) {
        return -1;
    }

    @Override
    public boolean removeHeadersEndHandler(int handlerID) {
        return false;
    }

    @Override
    public int addBodyEndHandler(Handler<Void> handler) {
        return -1;
    }

    @Override
    public boolean removeBodyEndHandler(int handlerID) {
        return false;
    }

    @Override
    public int addEndHandler(Handler<AsyncResult<Void>> handler) {
        return -1;
    }

    @Override
    public boolean removeEndHandler(int handlerID) {
        return false;
    }

    @Override
    public boolean failed() {
        return false;
    }

    @Override
    public void setAcceptableContentType(String contentType) {
    }

    @Override
    public void reroute(HttpMethod method, String path) {
    }

    @Override
    public Map<String, String> pathParams() {
        return Collections.emptyMap();
    }

    @Override
    public String pathParam(String name) {
        return null;
    }

    @Override
    public MultiMap queryParams() {
        return request.params();
    }

    @Override
    public MultiMap queryParams(Charset charset) {
        return request.params();
    }

    @Override
    public List<String> queryParam(String name) {
        return request.params().getAll(name);
    }
}
