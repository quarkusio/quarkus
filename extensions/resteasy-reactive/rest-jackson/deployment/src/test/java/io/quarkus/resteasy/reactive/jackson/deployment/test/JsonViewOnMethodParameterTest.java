package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.function.Supplier;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.test.QuarkusUnitTest;

public class JsonViewOnMethodParameterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(User.class, Views.class, Resource.class);
                }
            });

    @Test
    public void test() {
        given().accept("application/json")
                .contentType("application/json")
                .body("""
                        {
                         "id": 1,
                         "name": "Foo"
                        }
                        """)
                .post("test")
                .then()
                .statusCode(201)
                .body(not(containsString("1")), containsString("Foo"));
    }

    @Path("test")
    public static class Resource {

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        @Consumes(MediaType.APPLICATION_JSON)
        public RestResponse<User> create(@JsonView(Views.Public.class) User user) {
            return RestResponse.status(CREATED, user);
        }
    }
}
