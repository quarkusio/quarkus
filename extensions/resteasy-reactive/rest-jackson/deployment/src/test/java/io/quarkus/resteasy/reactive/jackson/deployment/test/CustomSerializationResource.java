package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.resteasy.reactive.jackson.CustomDeserialization;
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;

@CustomSerialization(CustomSerializationResource.UnquotedFieldsPersonSerialization.class)
@CustomDeserialization(CustomSerializationResource.UnquotedFieldsPersonDeserialization.class)
@Path("")
public class CustomSerializationResource {

    @ServerExceptionMapper
    public Response handleParseException(WebApplicationException e) {
        var cause = e.getCause() == null ? e : e.getCause();
        return Response.status(Response.Status.BAD_REQUEST).entity(cause.getMessage()).build();
    }

    @GET
    @Path("/custom-serialization/person")
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        person.setAddress("10 Downing St");
        person.setBirthDate("November 30, 1874");
        return person;
    }

    @POST
    @Path("/custom-serialization/person")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Person getPerson(Person person) {
        return person;
    }

    @POST
    @Path("/custom-serialization/people/list")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Person> getPeople(List<Person> people) {
        List<Person> reversed = new ArrayList<>(people.size());
        for (Person person : people) {
            reversed.add(0, person);
        }
        return reversed;
    }

    @GET
    @Path("/custom-serialization/invalid-use-of-custom-serializer")
    public User invalidUseOfCustomSerializer() {
        return testUser();
    }

    private User testUser() {
        User user = new User();
        user.id = 1;
        user.name = "test";
        return user;
    }

    public static class UnquotedFieldsPersonSerialization implements BiFunction<ObjectMapper, Type, ObjectWriter> {

        public static final AtomicInteger count = new AtomicInteger();

        public UnquotedFieldsPersonSerialization() {
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

    public static class UnquotedFieldsPersonDeserialization implements BiFunction<ObjectMapper, Type, ObjectReader> {

        public static final AtomicInteger count = new AtomicInteger();

        public UnquotedFieldsPersonDeserialization() {
            count.incrementAndGet();
        }

        @Override
        public ObjectReader apply(ObjectMapper objectMapper, Type type) {
            if (type instanceof ParameterizedType) {
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            if (!type.getTypeName().equals(Person.class.getName())) {
                throw new IllegalArgumentException("Only Person type can be handled");
            }
            return objectMapper.reader().with(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES);
        }
    }

}
