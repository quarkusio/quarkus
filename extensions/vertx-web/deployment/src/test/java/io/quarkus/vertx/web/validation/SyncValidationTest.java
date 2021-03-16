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
import io.vertx.core.http.HttpMethod;

public class SyncValidationTest {

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

        // HTML output
        get("/invalid")
                .then()
                .statusCode(500)
                .body(containsString("ConstraintViolation"), is(not(emptyString())));

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

        // Input parameter violation - HTML
        given()
                .queryParam("name", "doesNotMatch")
                .when()
                .get("/query")
                .then().statusCode(400)
                .body(containsString("ConstraintViolation"))
                .body(is(not(emptyString())));
    }

    @ApplicationScoped
    public static class MyRoutes {

        @Route(methods = HttpMethod.GET, path = "/valid")
        public Greeting getValidGreeting() {
            return new Greeting("luke", "hello");
        }

        @Route(methods = HttpMethod.GET, path = "/invalid")
        @Valid
        public Greeting getInvalidValidGreeting() {
            return new Greeting("neo", "hello");
        }

        @Route(methods = HttpMethod.GET, path = "/invalid2")
        public @Valid Greeting getDoubleInValidGreeting() {
            return new Greeting("neo", "hi");
        }

        @Route(methods = HttpMethod.GET, path = "/query")
        public Greeting getGreetingWithName(@Pattern(regexp = "ne.*") @NotNull @Param("name") String name) {
            return new Greeting(name, "hi");
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
