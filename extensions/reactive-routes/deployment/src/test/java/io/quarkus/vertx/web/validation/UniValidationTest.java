package io.quarkus.vertx.web.validation;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyString;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.smallrye.mutiny.Uni;

public class UniValidationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyRoutes.class));

    @Test
    public void test() {
        // Valid result
        get("/valid").then().statusCode(200)
                .body("name", is("luke"))
                .body("welcome", is("hello"));

        // Valid parameter
        given()
                .queryParam("name", "neo")
                .when()
                .get("/query")
                .then().statusCode(200);

        // JSON output
        given()
                .header("Accept", "application/json")
                .when()
                .get("/invalid")
                .then()
                .statusCode(500)
                .body("title", containsString("Constraint Violation"))
                .body("status", is(500))
                .body("details", containsString("validation constraint violations"))
                .body("violations[0].field", containsString("name"))
                .body("violations[0].message", is(not(emptyString())));

        // JSON output as it\s a Uni of object
        given()
                .when()
                .get("/invalid")
                .then()
                .statusCode(500)
                .body("title", containsString("Constraint Violation"))
                .body("status", is(500))
                .body("details", containsString("validation constraint violations"))
                .body("violations[0].field", containsString("name"))
                .body("violations[0].message", is(not(emptyString())));

        given()
                .header("Accept", "application/json")
                .when()
                .get("/invalid2").then().statusCode(500)
                .body("title", containsString("Constraint Violation"))
                .body("status", is(500))
                .body("details", containsString("validation constraint violations"))
                .body("violations[0].field", anyOf(containsString("name"), containsString("welcome")))
                .body("violations[0].message", is(not(emptyString())))
                .body("violations[1].field", anyOf(containsString("name"), containsString("welcome")))
                .body("violations[1].message", is(not(emptyString())));

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

        @Route(methods = HttpMethod.GET, path = "/valid")
        public Uni<Greeting> getValidGreeting() {
            return Uni.createFrom().item(new Greeting("luke", "hello"));
        }

        @Route(methods = HttpMethod.GET, path = "/invalid")
        @Valid
        public Uni<Greeting> getInvalidValidGreeting() {
            return Uni.createFrom().item(new Greeting("neo", "hello"));
        }

        @Route(methods = HttpMethod.GET, path = "/invalid2")
        public @Valid Uni<Greeting> getDoubleInValidGreeting() {
            return Uni.createFrom().item(new Greeting("neo", "hi"));
        }

        @Route(methods = HttpMethod.GET, path = "/query")
        public Uni<Greeting> getGreetingWithName(@Pattern(regexp = "ne.*") @NotNull @Param("name") String name) {
            return Uni.createFrom().item(new Greeting(name, "hi"));
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
