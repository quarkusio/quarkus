package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.core.Response;

import io.vertx.core.http.HttpClientResponse;

class RedirectUtil {

    private RedirectUtil() {
    }

    static Response toResponse(HttpClientResponse httpClientResponse) {
        Response.ResponseBuilder response = Response.status(httpClientResponse.statusCode());
        for (String headerName : httpClientResponse.headers().names()) {
            response.header(headerName, httpClientResponse.headers().get(headerName));
        }
        return response.build();
    }
}
