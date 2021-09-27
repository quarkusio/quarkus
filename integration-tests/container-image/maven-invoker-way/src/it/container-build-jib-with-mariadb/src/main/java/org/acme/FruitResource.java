package org.acme;

import io.smallrye.common.annotation.Blocking;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.persistence.EntityManager;
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
