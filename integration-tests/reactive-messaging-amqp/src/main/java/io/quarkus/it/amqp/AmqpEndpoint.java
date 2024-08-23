package io.quarkus.it.amqp;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/amqp")
public class AmqpEndpoint {

    @Inject
    PeopleManager people;

    @GET
    @Path("/people")
    public List<Person> getPeople() {
        return people.getPeople();
    }

}
