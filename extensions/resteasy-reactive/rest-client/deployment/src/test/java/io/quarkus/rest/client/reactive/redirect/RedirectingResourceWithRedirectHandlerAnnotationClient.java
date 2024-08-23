package io.quarkus.rest.client.reactive.redirect;

import java.net.URI;

import jakarta.ws.rs.core.Response;

import io.quarkus.rest.client.reactive.ClientRedirectHandler;

public interface RedirectingResourceWithRedirectHandlerAnnotationClient extends RedirectingResourceClient302 {
    @ClientRedirectHandler
    static URI alwaysRedirect(Response response) {
        if (response.getStatus() > 300) {
            return response.getLocation();
        }

        return null;
    }
}
