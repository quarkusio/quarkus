package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.BlockingOperationControl;

@Path("/simple")
public class SimpleJsonResource {

    @GET
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return person;
    }

    @POST
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person getPerson(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return person;
    }

    @POST
    @Path("/person-large")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person personTest(Person person) {
        //large requests should get bumped from the IO thread
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should have dispatched");
        }
        return person;
    }

    @POST
    @Path("/person-validated")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person getValidatedPerson(@Valid Person person) {
        return person;
    }

    @POST
    @Path("/person-invalid-result")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Valid
    public Person getInvalidPersonResult(@Valid Person person) {
        person.setLast(null);
        return person;
    }

    @GET
    @Path("/async-person")
    @Produces(MediaType.APPLICATION_JSON)
    public void getPerson(@Suspended AsyncResponse response) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Person person = new Person();
                person.setFirst("Bob");
                person.setLast("Builder");
                response.resume(person);
            }
        }).start();
    }

}
