package io.quarkus.it.spring.tx;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SpringTransactionalResourceTest {

    @Test
    public void testDefaultTransactional() {
        when().get("/spring-tx/default")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testRequiresNew() {
        when().get("/spring-tx/requires-new")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testSupportsWithoutExistingTransaction() {
        when().get("/spring-tx/supports")
                .then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    public void testNever() {
        when().get("/spring-tx/never")
                .then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    public void testClassLevelTransactional() {
        when().get("/spring-tx/class-level/one")
                .then()
                .statusCode(200)
                .body(is("true"));

        when().get("/spring-tx/class-level/two")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    public void testRollbackFor() {
        when().get("/spring-tx/rollback-for")
                .then()
                .statusCode(200)
                .body(is("ROLLED_BACK"));
    }
}
