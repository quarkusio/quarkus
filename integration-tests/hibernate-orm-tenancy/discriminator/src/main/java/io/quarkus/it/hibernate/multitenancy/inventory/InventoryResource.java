package io.quarkus.it.hibernate.multitenancy.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.quarkus.hibernate.orm.PersistenceUnit;

@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
@Path("/")
public class InventoryResource {

    @Inject
    @PersistenceUnit("inventory")
    EntityManager entityManager;

    @GET
    @Path("inventory")
    public Plane[] getDefault() {
        return get();
    }

    @GET
    @Path("{tenant}/inventory")
    public Plane[] getTenant() {
        return get();
    }

    private Plane[] get() {
        return entityManager.createNamedQuery("Planes.findAll", Plane.class)
                .getResultList().toArray(new Plane[0]);
    }
}
