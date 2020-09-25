package org.acme.resteasyjackson

import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("/resteasy-jackson/fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class FruitResource {

    private val fruits: MutableSet<Fruit> = mutableSetOf(
            Fruit("Apple", "Winter fruit"),
            Fruit("Pineapple", "Tropical fruit"),
            Fruit("Strawberry", null)
    )

    @GET
    fun list() = fruits

    @POST
    fun add(fruit: Fruit): Set<Fruit> {
        fruits.add(fruit)
        return fruits
    }

    @DELETE
    fun delete(fruit: Fruit): Set<Fruit> {
        fruits.removeIf { existingFruit: Fruit -> existingFruit.name.contentEquals(fruit.name) }
        return fruits
    }
}