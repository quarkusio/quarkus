package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class RawTypeReflectionFreeSerializerTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(RawTypeResource.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testRawMap() {
        RestAssured.get("/raw/map")
                .then()
                .statusCode(200)
                .body("key", is("value"));
    }

    @Test
    public void testRawList() {
        RestAssured.get("/raw/list")
                .then()
                .statusCode(200)
                .body("[0]", is("a"));
    }

    @Test
    public void testRawSet() {
        RestAssured.get("/raw/set")
                .then()
                .statusCode(200)
                .body("$", containsInAnyOrder("x", "y"));
    }

    @Path("/raw")
    public static class RawTypeResource {

        @SuppressWarnings("rawtypes")
        @GET
        @Path("/map")
        @Produces(MediaType.APPLICATION_JSON)
        public Map rawMap() {
            return Map.of("key", "value");
        }

        @SuppressWarnings("rawtypes")
        @GET
        @Path("/list")
        @Produces(MediaType.APPLICATION_JSON)
        public List rawList() {
            return List.of("a", "b", "c");
        }

        @SuppressWarnings("rawtypes")
        @GET
        @Path("/set")
        @Produces(MediaType.APPLICATION_JSON)
        public Set rawSet() {
            return Set.of("x", "y");
        }
    }
}
