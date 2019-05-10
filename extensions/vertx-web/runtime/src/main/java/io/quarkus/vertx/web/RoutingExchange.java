package io.quarkus.vertx.web;

import java.util.Optional;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Convenient wrapper of {@link RoutingContext}.
 */
public interface RoutingExchange {

    RoutingContext context();

    default HttpServerRequest request() {
        return context().request();
    }

    /**
     * 
     * @param paramName
     * @return the request parameter
     * @see HttpServerRequest#getParam(String)
     */
    default Optional<String> getParam(String paramName) {
        return Optional.ofNullable(request().getParam(paramName));
    }

    /**
     * 
     * @param paramName
     * @return the first header value with the specified name
     * @see HttpServerRequest#getHeader(CharSequence)
     */
    default Optional<String> getHeader(CharSequence headerName) {
        return Optional.ofNullable(request().getHeader(headerName));
    }

    default HttpServerResponse response() {
        return context().response();
    }

    default HttpServerResponse ok() {
        return response().setStatusCode(200);
    }

    default void ok(String chunk) {
        ok().end(chunk);
    }

    default HttpServerResponse serverError() {
        return response().setStatusCode(500);
    }

    default HttpServerResponse notFound() {
        return response().setStatusCode(404);
    }

}
