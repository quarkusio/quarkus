package io.quarkus.it.rabbitmq;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/rabbitmq")
public class RabbitMQEndpoint {
    @Inject
    PeopleManager people;

    @GET
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        return people.getPeople();
    }
}
