package io.quarkus.it.opentelemetry.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;

@Path("{dummy}/secured")
public class SecuredResource {
    @GET
    @Path("item/{value}")
    public String get(@RestPath String dummy, @RestPath String value) {
        return "Received: " + value;
    }
}
