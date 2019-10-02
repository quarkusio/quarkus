package org.acme.quickstart.stm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class STMResourceTest {

    @Test
    void testGet() {
        given()
                .when().get("/stm")
                .then()
                .statusCode(200);
    }

    @Test
    void testPost() {
        String responseString;

        makeBooking();
        responseString = makeBooking();

        System.out.printf("%s%n", responseString);
        assertThat(responseString, containsString("Booking Count=2"));
    }

    private String makeBooking() {
        return RestAssured.post("/stm").then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .asString();
    }
}
