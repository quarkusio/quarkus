package io.quarkus.hibernate.orm.rest.data.panache.deployment.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.Authenticated;

public class AuthenticatedClassPropertyTest extends AbstractSecurityAnnotationTest {

    @ResourceProperties(path = "items", authenticated = true)
    public interface ItemsResource extends PanacheEntityResource<Item, Long> {

        @Authenticated // we add it just to make sure that adding it doesn't break the generated code
        boolean delete(Long id);
    }

    @Test
    void test() {
        // list method is protected so we should get an HTTP 200 even if no user is specified
        given().accept("application/json")
                .when()
                .get("/items")
                .then()
                .statusCode(401);

        // count method is protected so we should get an HTTP 200 even if no user is specified
        given().accept("application/json")
                .when()
                .get("/items/count")
                .then()
                .statusCode(401);

        // delete method is also protected so we should get an HTTP 401 when no user is specified
        given().accept("application/json")
                .when()
                .delete("/items/1")
                .then()
                .statusCode(401);

        // list method is protected so we should get an HTTP 401 when a wrong username and password is specified
        given().auth().preemptive()
                .basic("foo", "foo2")
                .accept("application/json")
                .when()
                .delete("/items")
                .then()
                .statusCode(401);

        // list method is protected so we should get an HTTP 200 when the proper username and password are specified
        given().auth().preemptive()
                .basic("foo", "foo")
                .accept("application/json")
                .when()
                .get("/items")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }
}
