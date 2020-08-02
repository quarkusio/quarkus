package io.quarkus.it.rest;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("gouv-geo-api")
public interface GouvFrGeoApiClient {

    @GET
    @Path("/communes")
    public Set<Commune> getCommunes(
            @QueryParam("codePostal") String postalCode);
}