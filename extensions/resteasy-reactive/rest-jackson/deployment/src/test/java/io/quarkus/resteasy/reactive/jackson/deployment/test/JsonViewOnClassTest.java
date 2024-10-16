package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.util.function.Supplier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonView;

import io.quarkus.test.QuarkusUnitTest;

class JsonViewOnClassTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(User.class, Views.class, Public.class, Private.class, Mixed.class);
                }
            });

    @Test
    void test() {
        given().accept("application/json").get("public")
                .then()
                .statusCode(200)
                .body(not(containsString("1")), containsString("test"));

        given().accept("application/json").get("mixed")
                .then()
                .statusCode(200)
                .body(containsString("1"), containsString("test"));

        given().accept("application/json").get("private")
                .then()
                .statusCode(200)
                .body(containsString("1"), containsString("test"));
    }

    @JsonView(Views.Public.class)
    @Path("public")
    public static class Public {

        @GET
        public User get() {
            return User.testUser();
        }
    }

    @JsonView(Views.Private.class)
    @Path("private")
    public static class Private {

        @GET
        public User get() {
            return User.testUser();
        }
    }

    @JsonView(Views.Public.class)
    @Path("mixed")
    public static class Mixed {

        @GET
        @JsonView(Views.Private.class)
        public User get() {
            return User.testUser();
        }
    }
}
