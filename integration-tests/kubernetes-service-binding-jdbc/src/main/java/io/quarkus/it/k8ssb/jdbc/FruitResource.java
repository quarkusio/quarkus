package io.quarkus.it.k8ssb.jdbc;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
