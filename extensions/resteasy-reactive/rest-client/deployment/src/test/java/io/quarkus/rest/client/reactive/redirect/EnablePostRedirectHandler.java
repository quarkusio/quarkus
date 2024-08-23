package io.quarkus.rest.client.reactive.redirect;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;

import org.jboss.resteasy.reactive.client.handlers.RedirectHandler;

public class EnablePostRedirectHandler implements ContextResolver<RedirectHandler> {

    @Override
    public RedirectHandler getContext(Class<?> aClass) {
        return response -> {
            if (Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.REDIRECTION) {
                return response.getLocation();
            }
            return null;
        };
    }
}
