package io.quarkus.it.resteasy.jsonb;

import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/coffee")
public class CoffeeResource {

    @GET
    @Produces("application/json")
    public Coffee coffee() {
        Coffee coffee = new Coffee();
        coffee.setId(1);
        coffee.setName("Robusta");
        coffee.setCountryOfOrigin(new Country(1003, "Ethiopia", "ETH"));
        coffee.setSellers(Arrays.asList(new Seller("Carrefour", new Country(1001, "France", "FRA")),
                new Seller("Wallmart", new Country(1002, "USA", "USA"))));
        coffee.similarCoffees = Collections.singletonMap("arabica", 50);
        return coffee;
    }
}
