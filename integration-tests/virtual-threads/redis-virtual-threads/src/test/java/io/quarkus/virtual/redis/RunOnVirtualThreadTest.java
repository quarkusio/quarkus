package io.quarkus.virtual.redis;

import static org.hamcrest.Matchers.is;

import org.assertj.core.api.Assertions;
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
                .body(is("OK"));
    }

    @Test
    void testCache() {
        var value = RestAssured.get("/cached").then()
                .assertThat().statusCode(200)
                .extract().asPrettyString();

        var value2 = RestAssured.get("/cached").then()
                .assertThat().statusCode(200)
                .extract().asPrettyString();

        Assertions.assertThat(value).isEqualTo(value2);
    }
}
