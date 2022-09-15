package io.quarkus.it.k8ssb.jdbc;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

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
