package io.quarkus.resteasy.reactive.jackson.deployment.test;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class FullyFeaturedStreamConstraintsExceptionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FroMage.class, Views.class, Resource.class);
                }
            });

    @Test
    public void test() {
        String bigNumber = "9".repeat(1001);
        RestAssured.with()
                .contentType("application/json")
                .body("{\"price\": " + bigNumber + "}")
                .put("/fromage")
                .then().statusCode(400);
    }

    @Path("fromage")
    public static class Resource {

        @PUT
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public FroMage put(@JsonView(Views.Public.class) FroMage fromage) {
            return fromage;
        }
    }
}
