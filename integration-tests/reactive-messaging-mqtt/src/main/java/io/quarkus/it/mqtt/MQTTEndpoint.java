package io.quarkus.it.mqtt;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/mqtt")
public class MQTTEndpoint {
    @Inject
    PeopleManager people;

    @GET
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getPeople() {
        return people.getPeople();
    }

    @POST
    @Path("/people")
    @Produces(MediaType.APPLICATION_JSON)
    public void seedPeople() {
        people.seedPeople();
    }
}
