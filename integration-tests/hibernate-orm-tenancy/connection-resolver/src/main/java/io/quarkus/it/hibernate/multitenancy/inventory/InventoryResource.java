package io.quarkus.it.hibernate.multitenancy.inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
