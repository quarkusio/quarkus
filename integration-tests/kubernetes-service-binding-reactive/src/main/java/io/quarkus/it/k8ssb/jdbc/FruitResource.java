package io.quarkus.it.k8ssb.jdbc;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.hibernate.reactive.mutiny.Mutiny;

import io.smallrye.mutiny.Uni;

@Path("fruits")
public class FruitResource {

    @Inject
    Mutiny.SessionFactory sf;

    @GET
    @Produces("application/json")
    @Consumes("application/json")
    public Uni<List<Fruit>> get() {
        return sf.withSession(session -> session.createNamedQuery("Fruits.findAll", Fruit.class).getResultList());
    }
}
