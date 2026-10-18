package io.quarkus.it.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("gouv-geo-api")
public class GouvFrGeoApiClientImpl {

    @GET
    @Path("/communes")
    public Set<Commune> getCommunes(
            @QueryParam("codePostal") String postalCode) {
        Set<Commune> ret = new HashSet<>();
        Set<String> cp = new HashSet<>(Arrays.asList("75001", "75002", "75003", "75004", "75005", "75006", "75007", "75008",
                "75009", "75010", "75011", "75012", "75013", "75014", "75015", "75016", "75017", "75018", "75019", "75020"));
        ret.add(new Commune("Paris", "75056", "75", "11", cp, 2190327));
        return ret;
    }
}
