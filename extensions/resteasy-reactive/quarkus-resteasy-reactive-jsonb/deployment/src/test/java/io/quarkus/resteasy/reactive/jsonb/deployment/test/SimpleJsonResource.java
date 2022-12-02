package io.quarkus.resteasy.reactive.jsonb.deployment.test;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;

@Path("/simple")
@NonBlocking
public class SimpleJsonResource extends SuperClass<Person> {

    @GET
    @Path("/person")
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
    @Path("/person-custom-mt")
    @Produces("application/vnd.quarkus.person-v1+json")
    @Consumes("application/vnd.quarkus.person-v1+json")
    public Person getPersonCustomMediaType(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return person;
    }

    @POST
    @Path("/person-custom-mt-response")
    @Produces("application/vnd.quarkus.person-v1+json")
    @Consumes("application/vnd.quarkus.person-v1+json")
    public Response getPersonCustomMediaTypeResponse(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return Response.ok(person).status(201).build();
    }

    @POST
    @Path("/person-custom-mt-response-with-type")
    @Produces("application/vnd.quarkus.person-v1+json")
    @Consumes("application/vnd.quarkus.person-v1+json")
    public Response getPersonCustomMediaTypeResponseWithType(Person person) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return Response.ok(person).status(201).header("Content-Type", "application/vnd.quarkus.other-v1+json").build();
    }

    @POST
    @Path("/people")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Person> getPeople(List<Person> people) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        List<Person> reversed = new ArrayList<>(people.size());
        for (Person person : people) {
            reversed.add(0, person);
        }
        return reversed;
    }

    @POST
    @Path("/strings")
    public List<String> strings(List<String> strings) {
        if (BlockingOperationControl.isBlockingAllowed()) {
            throw new RuntimeException("should not have dispatched");
        }
        return strings;
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

    @GET
    @Path("/multi1")
    public Multi<Person> getMulti1() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return Multi.createFrom().items(person);
    }

    @GET
    @Path("/multi2")
    public Multi<Person> getMulti2() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        Person person2 = new Person();
        person2.setFirst("Bob2");
        person2.setLast("Builder2");
        return Multi.createFrom().items(person, person2);
    }

    @GET
    @Path("/multi0")
    public Multi<Person> getMulti0() {
        return Multi.createFrom().empty();
    }

    @POST
    @Path("/genericInput")
    public String genericInputTest(DataItem<Item> item) {
        return item.getContent().getName();
    }
}
