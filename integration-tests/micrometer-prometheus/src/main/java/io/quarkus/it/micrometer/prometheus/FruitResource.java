package io.quarkus.it.micrometer.prometheus;

import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.smallrye.common.annotation.Blocking;

@Path("/fruit")
@Blocking
public class FruitResource {

    @GET
    @Path("create")
    @Transactional
    public void trigger() {
        Fruit apple = new Fruit("apple");
        Fruit pear = new Fruit("pear");
        Fruit banana = new Fruit("banana");

        Fruit.persist(apple, pear, banana);
    }

    @GET
    @Path("all")
    public void retrieveAll() {
        PanacheQuery<Fruit> query = Fruit.find(
                "select name from Fruit");
        List<Fruit> all = query.list();
    }
}
