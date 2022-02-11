package com.example.postgresql.devservice;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/persons")
@Produces(MediaType.APPLICATION_JSON)
public class PersonResource {

    @GET
    public Response findAllPersons() {
        return Response.ok(Person.findAll().list()).build();
    }

}
