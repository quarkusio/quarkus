package io.quarkus.resteasy.reactive.server.test.devmode;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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
