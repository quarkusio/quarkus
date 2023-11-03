package io.quarkus.it.k8ssb.jdbc;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import io.smallrye.common.annotation.Blocking;

@Path("fruits")
public class FruitResource {

    @Inject
    EntityManager entityManager;

    @GET
    @Blocking
    @Produces("application/json")
    @Consumes("application/json")
    public List<Fruit> get() {
        return entityManager.createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList();
    }
}
