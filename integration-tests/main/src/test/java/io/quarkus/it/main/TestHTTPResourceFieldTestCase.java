package io.quarkus.it.main;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.TestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TestHTTPResourceFieldTestCase {

    @TestHTTPEndpoint(TestResource.class)
    @TestHTTPResource("")
    URL testRootEndpoint;

    @TestHTTPEndpoint(TestResource.class)
    @TestHTTPResource("/")
    URL testRootEndpoint2;

    String testRootExpectedResponse = "TEST";

    @TestHTTPEndpoint(TestResource.class)
    @TestHTTPResource("int/10")
    URL testPathEndpoint;

    int testPathExpectedResponse = 11;

    @Test
    public void shouldConfigURLWithTestHTTPEndpointFromField() {
        given()
                .when().get(testRootEndpoint).then()
                .body(is(testRootExpectedResponse));

        given()
                .when().get(testRootEndpoint2).then()
                .body(is(testRootExpectedResponse));
    }

    @Test
    public void shouldConfigURLWithTestHTTPEndpointAndTestHTTPResourceFromField() {
        given()
                .when().get(testPathEndpoint).then()
                .body(is(String.valueOf(testPathExpectedResponse)));
    }

}
