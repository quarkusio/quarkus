package io.quarkus.it.amqp;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
