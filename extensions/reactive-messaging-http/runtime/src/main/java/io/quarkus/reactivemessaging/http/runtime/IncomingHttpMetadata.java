package io.quarkus.reactivemessaging.http.runtime;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

/**
 * Metadata for Http Source. Provides a way to get headers, method and path from a http request
 */
public class IncomingHttpMetadata {

    private final HttpServerRequest request;

    IncomingHttpMetadata(HttpServerRequest request) {
        this.request = request;
    }

    /**
     * http method of the request
     * 
     * @return either POST or PUT
     */
    public HttpMethod getMethod() {
        return request.method();
    }

    /**
     * headers of the request
     * 
     * @return a MultiMap of headers
     */
    public MultiMap getHeaders() {
        return request.headers();
    }

    /**
     * path of the request
     * 
     * @return path
     */
    public String getPath() {
        return request.path();
    }

    public HttpServerRequest unwrap() {
        return request;
    }
}
