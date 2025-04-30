package io.quarkus.virtual.disabled;

import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit5.virtual.ShouldNotPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;
import io.restassured.RestAssured;

@QuarkusTest
@VirtualThreadUnit
@ShouldNotPin
class RunOnVirtualThreadDisabledTest {

    @Test
    void testGet() {
        RestAssured.get().then()
                .assertThat().statusCode(200)
                .body(is("hello-1"));
        RestAssured.get().then()
                .assertThat().statusCode(200)
                // Same value - request scoped bean
                .body(is("hello-1"));
    }

    @Test
    void testPost() {
        var body1 = UUID.randomUUID().toString();
        var body2 = UUID.randomUUID().toString();
        RestAssured
                .given().body(body1)
                .post().then()
                .assertThat().statusCode(200)
                .body(is(body1 + "-1"));
        RestAssured
                .given().body(body2)
                .post().then()
                .assertThat().statusCode(200)
                // Same value - request scoped bean
                .body(is(body2 + "-1"));
    }

    @Test
    void testNonBlocking() {
        // Non Blocking
        RestAssured.get("/non-blocking").then()
                .assertThat().statusCode(200)
                .body(is("ok"));
        // Uni
        RestAssured.get("/uni").then()
                .assertThat().statusCode(200)
                .body(is("ok"));
        // Multi
        RestAssured.get("/multi").then()
                .assertThat().statusCode(200)
                .body(is("ok"));
    }

    @Test
    void testRegularBlocking() {
        RestAssured.get("/blocking").then()
                .assertThat().statusCode(200)
                .body(is("hello-1"));
    }

    @Test
    void testRunOnVirtualThreadOnClass() {
        RestAssured.get("/class").then()
                .assertThat().statusCode(200)
                .body(is("hello-1"));
        RestAssured.get("/class").then()
                .assertThat().statusCode(200)
                .body(is("hello-1"));

        RestAssured.get("/class/uni").then()
                .assertThat().statusCode(200)
                .body(is("ok"));

        RestAssured.get("/class/multi").then()
                .assertThat().statusCode(200)
                .body(is("ok"));
    }

    @Test
    void testFilters() {
        RestAssured.get("/filter").then()
                .assertThat().statusCode(200);
    }

}
