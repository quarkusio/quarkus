package io.quarkus.it.spring.boot;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BeanPropertiesTest {

    @Test
    void shouldHaveFinalValue() {
        when().get("/bean/finalValue")
                .then()
                .body(is(equalTo("final")));
    }

    @Test
    void shouldHavePackagePrivateValue() {
        when().get("/bean/packagePrivateValue")
                .then()
                .body(is(equalTo("100")));
    }

    @Test
    void shouldHaveValue() {
        when().get("/bean/value")
                .then()
                .body(is(equalTo("1")));
    }

    @Test
    void shouldHaveInnerClassValue() {
        when().get("/bean/innerClass/value")
                .then()
                .body(is(equalTo("inner-class-value")));
    }
}
