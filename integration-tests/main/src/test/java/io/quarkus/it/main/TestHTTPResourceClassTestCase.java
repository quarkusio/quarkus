package io.quarkus.it.main;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.GreetingEndpoint;
import io.quarkus.it.rest.TestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(TestResource.class)
public class TestHTTPResourceClassTestCase {

    URL testRootEndpoint;

    @TestHTTPResource("int/10")
    URL testPathEndpoint;

    @TestHTTPEndpoint(GreetingEndpoint.class)
    URL externalGreetingEndpoint;

    @TestHTTPEndpoint(GreetingEndpoint.class)
    @TestHTTPResource("name")
    URL externalGreetingNameEndpoint;

    @Test
    public void shouldConfigURLWithTestHTTPEndpointOnClass() {
        given()
                .when().get(testRootEndpoint).then()
                .body(is("TEST"));
    }

    @Test
    public void shouldConfigURLWithTestHTTPEndpointOnClassAndTestHTTPResourceOnField() {
        given()
                .when().get(testPathEndpoint).then()
                .body(is("11"));
    }

    @Test
    public void shouldConfigURLAndOverrideTestHTTPEndpointOnClass() {
        given()
                .when().get(externalGreetingEndpoint.toString() + "/anotherName").then()
                .body(is("Hello anotherName"));

        given()
                .when().get(externalGreetingNameEndpoint).then()
                .body(is("Hello name"));
    }

}
