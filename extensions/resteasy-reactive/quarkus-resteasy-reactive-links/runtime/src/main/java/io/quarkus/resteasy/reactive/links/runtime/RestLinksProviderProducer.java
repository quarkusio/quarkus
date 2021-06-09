package io.quarkus.resteasy.reactive.links.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.core.UriInfo;

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
