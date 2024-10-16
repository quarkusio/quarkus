package org.jboss.resteasy.reactive.client.impl;

import java.net.URI;
import java.util.function.Function;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.client.handlers.RedirectHandler;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;

class WrapperVertxRedirectHandlerImpl implements Function<HttpClientResponse, Future<RequestOptions>> {

    private final RedirectHandler redirectHandler;

    WrapperVertxRedirectHandlerImpl(RedirectHandler redirectHandler) {
        this.redirectHandler = redirectHandler;
    }

    @Override
    public Future<RequestOptions> apply(HttpClientResponse httpClientResponse) {
        Response jaxRsResponse = RedirectUtil.toResponse(httpClientResponse);

        URI newLocation = redirectHandler.handle(jaxRsResponse);
        if (newLocation != null) {
            RequestOptions options = new RequestOptions();
            options.setAbsoluteURI(newLocation.toString());
            if (httpClientResponse.statusCode() == 307) {
                // According to https://www.rfc-editor.org/rfc/rfc9110#status.307
                // HTTP 307 should not change the request method
                options.setMethod(httpClientResponse.request().getMethod());
            }
            return Future.succeededFuture(options);
        }

        // otherwise, no redirect
        return null;
    }
}
