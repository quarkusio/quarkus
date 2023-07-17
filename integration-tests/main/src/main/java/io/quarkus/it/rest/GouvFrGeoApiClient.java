package io.quarkus.it.rest;

import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("gouv-geo-api")
public interface GouvFrGeoApiClient {

    @GET
    @Path("/communes")
    public Set<Commune> getCommunes(
            @QueryParam("codePostal") String postalCode);
}
