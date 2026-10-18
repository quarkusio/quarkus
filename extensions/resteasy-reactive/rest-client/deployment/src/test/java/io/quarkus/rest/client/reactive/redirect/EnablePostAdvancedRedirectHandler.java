package io.quarkus.rest.client.reactive.redirect;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;

import org.jboss.resteasy.reactive.client.handlers.AdvancedRedirectHandler;

import io.vertx.core.http.RequestOptions;

public class EnablePostAdvancedRedirectHandler implements ContextResolver<AdvancedRedirectHandler> {

    @Override
    public AdvancedRedirectHandler getContext(Class<?> aClass) {
        return context -> {
            Response response = context.jaxRsResponse();
            if (Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.REDIRECTION) {
                var result = new RequestOptions();
                result.setAbsoluteURI(response.getLocation().toString());
                result.addHeader("x-foo", "bar");
                return result;
            }
            return null;
        };
    }
}
