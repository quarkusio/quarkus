package org.acme;

import io.smallrye.common.annotation.Blocking;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.persistence.EntityManager;
import java.util.List;

@Path("fruits")
@Blocking
public class FruitResource {

    @Inject
    EntityManager entityManager;

    @GET
    public List<Fruit> get() {
        return entityManager.createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList();
    }

}
