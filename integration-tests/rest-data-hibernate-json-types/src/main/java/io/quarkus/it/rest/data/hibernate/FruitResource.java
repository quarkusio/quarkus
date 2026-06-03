package io.quarkus.it.rest.data.hibernate;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("fruits")
public class FruitResource {

    private final FruitRepository repo;

    public FruitResource(FruitRepository repo) {
        this.repo = repo;
    }

    @GET
    public Page<Fruit> list(PageRequest pageRequest, Order<Fruit> order) {
        return repo.findAll(pageRequest, order);
    }
}
