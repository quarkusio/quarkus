package io.quarkus.it.hibernate.validator.restclient.server;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.it.hibernate.validator.restclient.RestClientEntity;

@Path("/rest-client")
public class ServerResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String doSomething(String parameter) {
        return parameter;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public RestClientEntity doSomething() {
        return new RestClientEntity(1, "aa");
    }
}
