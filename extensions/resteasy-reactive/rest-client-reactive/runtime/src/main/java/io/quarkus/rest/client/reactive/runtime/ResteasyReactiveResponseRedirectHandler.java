package io.quarkus.rest.client.reactive.runtime;

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;

import org.jboss.resteasy.reactive.client.handlers.RedirectHandler;

public interface ResteasyReactiveResponseRedirectHandler extends ContextResolver<RedirectHandler> {

    default URI handle(Response response) {
        throw new IllegalStateException("should never be invoked");
    }

    @Override
    default RedirectHandler getContext(Class<?> aClass) {
        return this::handle;
    }
}
