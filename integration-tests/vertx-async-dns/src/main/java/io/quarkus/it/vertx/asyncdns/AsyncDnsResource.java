package io.quarkus.it.vertx.asyncdns;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.Vertx;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.spi.dns.AddressResolverProvider;

@Path("/dns")
public class AsyncDnsResource {

    @Inject
    Vertx vertx;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String resolver() {
        AddressResolverProvider provider = AddressResolverProvider.factory(vertx, new AddressResolverOptions());
        try {
            return provider.getClass().getName();
        } finally {
            provider.close();
        }
    }
}
