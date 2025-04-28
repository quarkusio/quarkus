package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

class OverlappingResourceClassPathTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClasses(UsersResource.class);
                    war.addClasses(UserResource.class);
                    war.addClasses(GreetingResource.class);
                    return war;
                }
            });

    @Test
    void basicTest() {
        given()
                .get("/users/userId")
                .then()
                .statusCode(200)
                .body(equalTo("userId"));

        given()
                .get("/users/userId/by-id")
                .then()
                .statusCode(200)
                .body(equalTo("getByIdInUserResource-userId"));
    }

    @Path("/users")
    public static class UsersResource {

        @GET
        @Path("{id}")
        public String getByIdInUsersResource(@RestPath String id) {
            return id;
        }
    }

    @Path("/users/{id}")
    public static class UserResource {

        @GET
        @Path("by-id")
        public String getByIdInUserResource(@RestPath String id) {
            return "getByIdInUserResource-" + id;
        }
    }

    @Path("/i-do-not-match")
    public static class GreetingResource {

        @GET
        @Path("greet")
        public String greet() {
            return "Hello";
        }
    }
}
