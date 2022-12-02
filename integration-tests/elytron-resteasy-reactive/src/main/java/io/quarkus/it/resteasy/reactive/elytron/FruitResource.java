package io.quarkus.it.resteasy.reactive.elytron;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;

@Path("fruit")
public class FruitResource {

    private final List<Fruit> fruits = List.of(Fruit.APPLE, Fruit.PINEAPPLE);

    @GET
    @Path("all-no-security")
    @DisableSecureSerialization
    public List<Fruit> allNoSecurity() {
        return fruits;
    }

    @GET
    @Path("all-with-security")
    public List<Fruit> allWithSecurity() {
        return fruits;
    }

    @GET
    @Path("single-no-security")
    @DisableSecureSerialization
    public Fruit singleNoSecurity() {
        return fruits.get(0);
    }

    @GET
    @Path("single-with-security")
    public Fruit singleWithSecurity() {
        return fruits.get(0);
    }
}
