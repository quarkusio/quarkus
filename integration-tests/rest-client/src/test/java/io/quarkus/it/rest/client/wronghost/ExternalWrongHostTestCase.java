package io.quarkus.it.rest.client.wronghost;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.RestClientTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(RestClientTestResource.class)
public class ExternalWrongHostTestCase {

    @Test
    public void restClient() {
        RestAssured.when()
                .get("/wrong-host/rest-client")
                .then()
                .statusCode(200)
                .body(is(not(empty())));
    }
}
