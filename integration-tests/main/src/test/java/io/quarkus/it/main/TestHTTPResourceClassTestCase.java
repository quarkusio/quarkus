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

    @TestHTTPResource("")
    URL testRootEndpoint;

    @TestHTTPResource("/")
    URL testRootEndpoint2;

    String testRootExpectedResponse = "TEST";

    @TestHTTPResource("int/10")
    URL testPathEndpoint;

    int testPathExpectedResponse = 11;

    @TestHTTPEndpoint(GreetingEndpoint.class)
    @TestHTTPResource("")
    URL overridenGreetingEndpoint;

    @TestHTTPEndpoint(GreetingEndpoint.class)
    @TestHTTPResource("name")
    URL overridenGreetingNameEndpoint;

    String overridenGreetingNameExpectedResponse = "Hello name";

    @Test
    public void shouldConfigURLWithTestHTTPEndpointFromClass() {
        given()
                .when().get(testRootEndpoint).then()
                .body(is(testRootExpectedResponse));

        given()
                .when().get(testRootEndpoint2).then()
                .body(is(testRootExpectedResponse));

        given()
                .when().get(testPathEndpoint).then()
                .body(is(String.valueOf(testPathExpectedResponse)));
    }

    @Test
    public void shouldConfigURLWithTestHTTPEndpointFromField() {
        given()
                .when().get(overridenGreetingEndpoint.toString() + "/anotherName").then()
                .body(is("Hello anotherName"));

        given()
                .when().get(overridenGreetingNameEndpoint).then()
                .body(is(overridenGreetingNameExpectedResponse));
    }

}
