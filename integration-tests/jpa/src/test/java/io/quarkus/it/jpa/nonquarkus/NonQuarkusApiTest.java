package io.quarkus.it.jpa.nonquarkus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test usage of JPA in a way that is not recommended with Quarkus:
 * creating the entity manager manually, using `em.getTransaction()`, ...
 * <p>
 * This is mainly tested to ensure migration is relatively easy, like we test `persistence.xml` support:
 * we expect developers to move to `@Inject EntityManager em;`
 * and `QuarkusTransaction`/`UserTransaction` for best results.
 */
@QuarkusTest
public class NonQuarkusApiTest {

    @Test
    public void test() {
        given().queryParam("expectedSchema", "SCHEMA1")
                .when().get("/jpa-test/non-quarkus/test").then()
                .body(is("OK"))
                .statusCode(200);
    }

}
