package io.quarkus.it.panache.reactive;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

/**
 * These tests cover for "mixed mode" usage of Panache Reactive from a blocking thread;
 * this is known to be tricky as Hibernate Reactive requires running on the event loop,
 * while Panache relies on the notion of "current Session" being stored in the current
 * CDI context.
 */
@Path("test")
public class TestEndpoint {

    @GET
    @Path("store3fruits")
    public String testStorage() {
        Fruit apple = new Fruit("apple", "red");
        Fruit orange = new Fruit("orange", "orange");
        Fruit banana = new Fruit("banana", "yellow");

        Panache.withTransaction(() -> Fruit.persist(apple, orange, banana)).subscribeAsCompletionStage().join();

        //We wants this same request to also perform a read, so to trigger a second lookup of the Mutiny.Session from ArC
        return verifyStored();
    }

    @GET
    @Path("load3fruits")
    public String verifyStored() {
        final List<PanacheEntityBase> fruitsList = Panache.withTransaction(() -> Fruit.find("select name, color from Fruit")
                .list())
                .subscribeAsCompletionStage()
                .join();
        return fruitsList.size() == 3 ? "OK" : "KO";
    }

}
