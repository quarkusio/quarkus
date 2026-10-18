package io.quarkus.resteasy.reactive.links.runtime;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.core.UriInfo;

import io.quarkus.arc.DefaultBean;
import io.quarkus.resteasy.reactive.links.RestLinksProvider;

@Dependent
public final class RestLinksProviderProducer {

    @Produces
    @RequestScoped
    @DefaultBean
    public RestLinksProvider restLinksProvider(UriInfo uriInfo) {
        return new RestLinksProviderImpl(uriInfo);
    }
}
