package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

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

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import io.quarkus.runtime.BlockingOperationControl;
import io.smallrye.mutiny.Multi;

@Path("/simple")
public class SimpleJsonResource extends SuperClass<Person> {

    @ServerExceptionMapper
    public Response handleParseException(JsonParseException jpe) {
        return Response.status(Response.Status.BAD_REQUEST).entity(jpe.getMessage()).build();
    }

    @GET
    @Path("/person")
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return person;
    }

    @CustomSerialization(UnquotedFieldsPersonBiFunction.class)
    @GET
    @Path("custom-serialized-person")
    public Person getCustomSerializedPerson() {
        return getPerson();
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

    @CustomSerialization(UnquotedFieldsPersonBiFunction.class)
    @POST
    @Path("/custom-serialized-people")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Person> getCustomSerializedPeople(List<Person> people) {
        return getPeople(people);
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
    @Path("/user-without-view")
    public User userWithoutView() {
        return testUser();
    }

    @JsonView(Views.Public.class)
    @GET
    @Path("/user-with-public-view")
    public User userWithPublicView() {
        return testUser();
    }

    @JsonView(Views.Private.class)
    @GET
    @Path("/user-with-private-view")
    public User userWithPrivateView() {
        return testUser();
    }

    @CustomSerialization(UnquotedFieldsPersonBiFunction.class)
    @GET
    @Path("/invalid-use-of-custom-serializer")
    public User invalidUseOfCustomSerializer() {
        return testUser();
    }

    private User testUser() {
        User user = new User();
        user.id = 1;
        user.name = "test";
        return user;
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

    @GET
    @Path("/multiString")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<String> getMultiString() {
        return Multi.createFrom().items("Hello", "Stu");
    }

    @POST
    @Path("/genericInput")
    public String genericInputTest(DataItem<Item> item) {
        return item.getContent().getName();
    }

    public static class UnquotedFieldsPersonBiFunction implements BiFunction<ObjectMapper, Type, ObjectWriter> {

        public static final AtomicInteger count = new AtomicInteger();

        public UnquotedFieldsPersonBiFunction() {
            count.incrementAndGet();
        }

        @Override
        public ObjectWriter apply(ObjectMapper objectMapper, Type type) {
            if (type instanceof ParameterizedType) {
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            if (!type.getTypeName().equals(Person.class.getName())) {
                throw new IllegalArgumentException("Only Person type can be handled");
            }
            return objectMapper.writer().without(JsonWriteFeature.QUOTE_FIELD_NAMES);
        }
    }
}
