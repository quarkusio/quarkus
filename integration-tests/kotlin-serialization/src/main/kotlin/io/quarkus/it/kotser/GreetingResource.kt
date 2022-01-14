package io.quarkus.it.kotser

import io.quarkus.it.kotser.model.Person
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("/")
class GreetingResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun hello(): Person {
        return Person("Jim Halpert")
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun marry(person: Person): Person {
        return Person(person.name.substringBefore(" ") + " Halpert")
    }
}

