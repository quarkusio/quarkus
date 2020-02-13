package io.quarkus.it.jpa.integrator;

import static io.restassured.RestAssured.when;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JPAIntegratorTest {

    @Test
    public void testInjection() {
        when().get("/jpa-test/integrator").then()
                .statusCode(200)
                .body(is("1"));
    }

}
