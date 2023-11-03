package io.quarkus.resteasy.reactive.server.test.simple;

import static org.hamcrest.CoreMatchers.is;

import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomContextTypeTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestResource.class, CustomType.class, CustomTypeProducer.class);
                }
            });

    @Test
    public void firstTest() {
        RestAssured.given().headers("foo", "bar")
                .get("/test")
                .then().statusCode(200).body(is("bar"));
    }

    @Test
    public void secondTest() {
        RestAssured.given().headers("foo", "baz")
                .get("/test")
                .then().statusCode(200).body(is("baz"));
    }

    @Path("/test")
    public static class TestResource {

        @GET
        public String get(@Context CustomType customType) {
            return customType.getValue();
        }
    }

    public static class CustomType {
        private final String value;

        public CustomType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Singleton
    public static class CustomTypeProducer {

        @Produces
        @RequestScoped
        public CustomType customType(HttpHeaders headers) {
            return new CustomType(headers.getHeaderString("foo"));
        }
    }
}
