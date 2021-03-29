package io.quarkus.it.rest.client.wronghost;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ExternalWrongHostTestResource.class)
public class ExternalWrongHostTestCase {

    @Test
    public void restClient() {
        when()
                .get("/wrong-host")
                .then()
                .statusCode(200)
                .body(is("200"));

        given()
                .when().get("/q/metrics")
                .then()
                .statusCode(200)
                .body(containsString(
                        "http_client_requests_seconds_count{clientName=\"wrong.host.badssl.com\",method=\"GET\",outcome=\"SUCCESS\",status=\"200\",uri=\"root\",}"));
    }
}
