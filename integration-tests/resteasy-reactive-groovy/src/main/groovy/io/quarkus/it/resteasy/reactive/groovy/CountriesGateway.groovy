package io.quarkus.it.resteasy.reactive.groovy

import io.smallrye.mutiny.Uni
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/v2")
@RegisterRestClient(configKey = "countries")
interface CountriesGateway {

    @GET @Path("/name/{name}") Uni<Set<Country>> byName(@PathParam("name") String name)
}
