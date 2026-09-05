package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.with;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonFilter;

import io.quarkus.resteasy.reactive.jackson.CustomDeserialization;
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.http.ContentType;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

public class ClassLevelCustomSerializationMultipleResourcesTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Account.class, Person.class, AdminSerializer.class, PublicSerializer.class,
                                    AdminAccountResource.class, PublicAccountResource.class,
                                    LenientDeserializer.class, StrictDeserializer.class,
                                    LenientPersonResource.class, StrictPersonResource.class);
                }
            });

    @Test
    public void testSerialization() {
        // the order matters: the writer of the first resource being hit used to be cached for the entity type
        // and then reused by the second resource
        with().accept(ContentType.JSON)
                .get("admin/account")
                .then()
                .statusCode(200)
                .body(containsString("alice"))
                .body(containsString("ssn"))
                .body(containsString("apiKey"));

        with().accept(ContentType.JSON)
                .get("public/account")
                .then()
                .statusCode(200)
                .body(containsString("alice"))
                .body(not(containsString("ssn")))
                .body(not(containsString("apiKey")));

        // make sure the admin resource still uses its own writer
        with().accept(ContentType.JSON)
                .get("admin/account")
                .then()
                .statusCode(200)
                .body(containsString("alice"))
                .body(containsString("ssn"))
                .body(containsString("apiKey"));
    }

    @Test
    public void testDeserialization() {
        String unquotedBody = "{first: \"bob\", last: \"builder\"}";

        // the order matters: the reader of the first resource being hit used to be cached for the entity type
        // and then reused by the second resource
        with().body(unquotedBody)
                .contentType(ContentType.JSON)
                .post("lenient/person")
                .then()
                .statusCode(200)
                .body(containsString("bob"));

        with().body(unquotedBody)
                .contentType(ContentType.JSON)
                .post("strict/person")
                .then()
                .statusCode(400);

        // make sure the lenient resource still uses its own reader
        with().body(unquotedBody)
                .contentType(ContentType.JSON)
                .post("lenient/person")
                .then()
                .statusCode(200)
                .body(containsString("bob"));
    }

    @JsonFilter("accountFilter")
    public static class Account {
        public String id = "acct-1";
        public String name = "alice";
        public String ssn = "078-05-1120";
        public String apiKey = "sk-live-SECRET";
    }

    public static class Person {
        public String first;
        public String last;
    }

    public static class AdminSerializer implements BiFunction<ObjectMapper, Type, ObjectWriter> {
        @Override
        public ObjectWriter apply(ObjectMapper mapper, Type type) {
            return mapper.writer().with(new SimpleFilterProvider()
                    .addFilter("accountFilter", SimpleBeanPropertyFilter.serializeAll()));
        }
    }

    public static class PublicSerializer implements BiFunction<ObjectMapper, Type, ObjectWriter> {
        @Override
        public ObjectWriter apply(ObjectMapper mapper, Type type) {
            return mapper.writer().with(new SimpleFilterProvider()
                    .addFilter("accountFilter", SimpleBeanPropertyFilter.serializeAllExcept("ssn", "apiKey")));
        }
    }

    public static class LenientDeserializer implements BiFunction<ObjectMapper, Type, ObjectReader> {
        @Override
        public ObjectReader apply(ObjectMapper mapper, Type type) {
            return mapper.reader().with(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES);
        }
    }

    public static class StrictDeserializer implements BiFunction<ObjectMapper, Type, ObjectReader> {
        @Override
        public ObjectReader apply(ObjectMapper mapper, Type type) {
            return mapper.reader().without(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES);
        }
    }

    @Path("/admin")
    @CustomSerialization(AdminSerializer.class)
    public static class AdminAccountResource {
        @GET
        @Path("/account")
        @Produces(MediaType.APPLICATION_JSON)
        public Account get() {
            return new Account();
        }
    }

    @Path("/public")
    @CustomSerialization(PublicSerializer.class)
    public static class PublicAccountResource {
        @GET
        @Path("/account")
        @Produces(MediaType.APPLICATION_JSON)
        public Account get() {
            return new Account();
        }
    }

    @Path("/lenient")
    @CustomDeserialization(LenientDeserializer.class)
    public static class LenientPersonResource {
        @POST
        @Path("/person")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Person post(Person person) {
            return person;
        }
    }

    @Path("/strict")
    @CustomDeserialization(StrictDeserializer.class)
    public static class StrictPersonResource {
        @POST
        @Path("/person")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Person post(Person person) {
            return person;
        }
    }
}
