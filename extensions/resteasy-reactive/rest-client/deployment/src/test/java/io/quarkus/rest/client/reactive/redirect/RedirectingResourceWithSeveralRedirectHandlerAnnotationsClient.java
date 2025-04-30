package io.quarkus.rest.client.reactive.redirect;

import java.net.URI;

import jakarta.ws.rs.core.Response;

import io.quarkus.rest.client.reactive.ClientRedirectHandler;

public interface RedirectingResourceWithSeveralRedirectHandlerAnnotationsClient extends RedirectingResourceClient302 {
    // This handler should never be called because it has lower priority than `neverRedirect`.
    @ClientRedirectHandler(priority = -1)
    static URI alwaysRedirect(Response response) {
        return response.getLocation();
    }

    @ClientRedirectHandler(priority = 1)
    static URI neverRedirect(Response response) {
        return null;
    }
}
