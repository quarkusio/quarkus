package org.acme;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/fruits")
public class FruitResource {

    @Inject
    EntityManager em;

    @GET
    public List<Fruit> list() {
        return em.createQuery("from Fruit", Fruit.class).getResultList();
    }
}
