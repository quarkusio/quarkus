package io.quarkus.it.spring.data.jpa;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/catalog-value")
public class CatalogValueResource {

    private final CatalogValueRepository repository;

    private final FederalStateCatalogValueRepository federalStateRepository;

    public CatalogValueResource(CatalogValueRepository repository,
            FederalStateCatalogValueRepository federalStateRepository) {
        this.repository = repository;
        this.federalStateRepository = federalStateRepository;
    }

    @Path("/super/{key}")
    @GET
    @Produces("application/json")
    public CatalogValue findByKey(@PathParam("key") String key) {
        return repository.findByKey(key);
    }

    @Path("/federal-state/{key}")
    @GET
    @Produces("application/json")
    public CatalogValue findFederalStateByKey(@PathParam("key") String key) {
        return federalStateRepository.findByKey(key);
    }
}
