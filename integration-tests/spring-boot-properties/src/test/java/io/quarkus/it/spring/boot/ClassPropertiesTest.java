package io.quarkus.it.spring.boot;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ClassPropertiesTest {

    @Test
    void shouldHaveValue() {
        when().get("/class/value")
                .then()
                .body(is(equalTo("class-value")));
    }

    @Test
    void shouldHaveAnotherClassValue() {
        when().get("/class/anotherClass/value")
                .then()
                .body(is(equalTo("true")));
    }
}
