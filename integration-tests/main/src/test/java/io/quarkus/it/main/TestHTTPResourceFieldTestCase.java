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
    URL testRootEndpoint;

    @TestHTTPEndpoint(TestResource.class)
    @TestHTTPResource("int/10")
    URL testPathEndpoint;

    @Test
    public void shouldConfigURLWithTestHTTPEndpointOnField() {
        given()
                .when().get(testRootEndpoint).then()
                .body(is("TEST"));
    }

    @Test
    public void shouldConfigURLWithTestHTTPEndpointAndTestHTTPResourceOnField() {
        given()
                .when().get(testPathEndpoint).then()
                .body(is("11"));
    }

}
