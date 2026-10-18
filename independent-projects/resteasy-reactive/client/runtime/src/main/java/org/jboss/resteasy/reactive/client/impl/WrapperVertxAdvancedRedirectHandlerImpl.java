package org.jboss.resteasy.reactive.client.impl;

import java.util.function.Function;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.client.handlers.AdvancedRedirectHandler;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.RequestOptions;

class WrapperVertxAdvancedRedirectHandlerImpl implements Function<HttpClientResponse, Future<RequestOptions>> {

    private final AdvancedRedirectHandler redirectHandler;

    WrapperVertxAdvancedRedirectHandlerImpl(AdvancedRedirectHandler redirectHandler) {
        this.redirectHandler = redirectHandler;
    }

    @Override
    public Future<RequestOptions> apply(HttpClientResponse httpClientResponse) {
        Response jaxRsResponse = RedirectUtil.toResponse(httpClientResponse);

        var result = redirectHandler.handle(new AdvancedRedirectHandler.Context(jaxRsResponse, httpClientResponse.request()));
        if (result != null) {
            return Future.succeededFuture(result);
        }

        // otherwise, no redirect
        return null;
    }

}
