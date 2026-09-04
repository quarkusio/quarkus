package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.with;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.http.ContentType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ContextResolverMultipleMediaTypesTest {

    @Test
    public void testMixin() {
        with().accept(ContentType.JSON)
                .get("person")
                .then()
                .statusCode(200)
                .body(containsString("078-05-1120"))
                .body(containsString("some_number"))
                .body(not(containsString("ssn")))
                .body(containsString("alice"));
    }

    @Test
    public void testNoMixin() {
        with().accept("application/stream+json")
                .get("person")
                .then()
                .statusCode(200)
                .body(containsString("078-05-1120"))
                .body(containsString("ssn"))
                .body(not(containsString("some_number")))
                .body(containsString("alice"));
    }

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Person.class, SomeMixin.class, PublicMapperResolver.class,
                                    InternalMapperResolver.class);
                }
            });

    public static class Person {
        private String name;
        private String ssn;

        public Person() {
        }

        public Person(String name, String ssn) {
            this.name = name;
            this.ssn = ssn;
        }

        public String getName() {
            return name;
        }

        public String getSsn() {
            return ssn;
        }
    }

    @Path("/person")
    public static class PersonResource {
        @GET
        @Produces({ "application/json", "application/stream+json" })
        public Person get() {
            return new Person("alice", "078-05-1120");
        }
    }

    public static abstract class SomeMixin {
        @JsonProperty("some_number")
        public abstract String getSsn();
    }

    @Provider
    @Produces("application/json")
    public static class PublicMapperResolver implements ContextResolver<ObjectMapper> {
        private final ObjectMapper restricted = JsonMapper.builder().addMixIn(Person.class, SomeMixin.class).build();

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return restricted;
        }
    }

    @Provider
    @Produces("application/stream+json")
    public static class InternalMapperResolver implements ContextResolver<ObjectMapper> {
        private final ObjectMapper full = new ObjectMapper();

        @Override
        public ObjectMapper getContext(Class<?> type) {
            return full;
        }
    }
}
