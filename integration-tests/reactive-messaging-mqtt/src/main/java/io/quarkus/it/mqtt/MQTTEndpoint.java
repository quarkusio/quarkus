package io.quarkus.it.mqtt;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
