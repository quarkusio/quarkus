package io.quarkus.it.spring.data.jpa;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

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
