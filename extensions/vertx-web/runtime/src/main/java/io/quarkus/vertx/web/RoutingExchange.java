package io.quarkus.vertx.web;

import java.util.Optional;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Convenient wrapper of {@link RoutingContext}.
 */
public interface RoutingExchange {

    /**
     * @return the underlying Vert.x routing context.
     */
    RoutingContext context();

    /**
     * @return the HTTP request object
     */
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
     * @param headerName
     * @return the first header value with the specified name
     * @see HttpServerRequest#getHeader(CharSequence)
     */
    default Optional<String> getHeader(CharSequence headerName) {
        return Optional.ofNullable(request().getHeader(headerName));
    }

    /**
     * @return the HTTP response object
     */
    default HttpServerResponse response() {
        return context().response();
    }

    /**
     * Set the response status code to 200 and return the response.
     * You must call <code>HttpServerResponse.end()</code> afterwards to end the response.
     * 
     * @return the HTTP response object
     */
    default HttpServerResponse ok() {
        return response().setStatusCode(200);
    }

    /**
     * Set the response status code to 200, write a chunk of data to the response then ends it.
     * 
     * @param chunk
     */
    default void ok(String chunk) {
        ok().end(chunk);
    }

    /**
     * Set the response status code to 500 and return the response.
     * You must call <code>HttpServerResponse.end()</code> afterwards to end the response.
     * 
     * @return the HTTP response object
     */
    default HttpServerResponse serverError() {
        return response().setStatusCode(500);
    }

    /**
     * Set the response status code to 404 and return the response.
     * You must call <code>HttpServerResponse.end()</code> afterwards to end the response.
     * 
     * @return the HTTP response object
     */
    default HttpServerResponse notFound() {
        return response().setStatusCode(404);
    }

}
