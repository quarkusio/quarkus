package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test reflection around JPA entities
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@QuarkusTest
public class JPAEntityManagerInjectionTestCase {

    @Test
    public void testJpaEntityManagerInjection() throws Exception {
        RestAssured.when().get("/jpa/testjpaeminjection").then()
                .body(is("OK"));
    }

}
