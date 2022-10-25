package io.quarkus.resteasy.reactive.server.test.devmode;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class SubResourceDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class, SubResource.class);
                }

            });

    @Test
    public void test() {
        RestAssured.get("/test/sub")
                .then()
                .statusCode(200)
                .body(equalTo("hello"));
    }

    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static class Resource {
        @Context
        ResourceContext resourceContext;

        @Path("sub")
        public SubResource subresource() {
            return new SubResource();
        }
    }

    public static class SubResource {

        @GET
        public String hello() {
            return "hello";
        }
    }
}
