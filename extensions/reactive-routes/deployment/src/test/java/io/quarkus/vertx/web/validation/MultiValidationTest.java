package io.quarkus.vertx.web.validation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.emptyString;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.smallrye.mutiny.Multi;

public class MultiValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyRoutes.class));

    @Test
    public void test() {

        // Valid parameter
        given()
                .queryParam("name", "neo")
                .when()
                .get("/query")
                .then().statusCode(200);

        // Input parameter violation - JSON
        given()
                .header("Accept", "application/json")
                .queryParam("name", "doesNotMatch")
                .when()
                .get("/query")
                .then().statusCode(400)
                .body("title", containsString("Constraint Violation"))
                .body("status", is(400))
                .body("details", containsString("validation constraint violations"))
                .body("violations[0].field", containsString("name"))
                .body("violations[0].message", is(not(emptyString())));
    }

    @ApplicationScoped
    public static class MyRoutes {

        @Route(methods = HttpMethod.GET, path = "/query")
        public Multi<Greeting> getGreetingWithName(@Pattern(regexp = "ne.*") @NotNull @Param("name") String name) {
            return Multi.createFrom().item(new Greeting(name, "hi"));
        }

    }

    public static class Greeting {

        private String name;
        private String welcome;

        public Greeting(String name, String welcome) {
            this.name = name;
            this.welcome = welcome;
        }

        @Length(min = 4)
        public String getName() {
            return name;
        }

        @Length(min = 4)
        public String getWelcome() {
            return welcome;
        }
    }

}
