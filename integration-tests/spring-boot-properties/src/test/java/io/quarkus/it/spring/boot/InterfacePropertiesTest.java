package io.quarkus.it.spring.boot;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class InterfacePropertiesTest {

    @Test
    void shouldHaveInt() {
        when().get("/interface/value")
                .then()
                .body(is(equalTo("interface-value")));
    }
}
