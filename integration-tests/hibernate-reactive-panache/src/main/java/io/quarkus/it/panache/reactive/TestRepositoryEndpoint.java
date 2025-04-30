package io.quarkus.it.panache.reactive;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.mutiny.Uni;

@Path("test-repo")
public class TestRepositoryEndpoint {

    @Inject
    BeerRepository beerRepository;

    // @WithSessionOnDemand is added automatically
    @GET
    @Path("beers")
    public Uni<String> testBeers() {
        return beerRepository.count().map(v -> "OK");
    }

}
