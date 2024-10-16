package io.quarkus.virtual.rest;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadTest {

    @Test
    void test() {
        RestAssured.get().then()
                .assertThat().statusCode(200)
                .body("message", is("hello"));
    }

}
