package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.it.faulttolerance.FaultToleranceTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class FaultToleranceTestCase {

    @TestHTTPEndpoint(FaultToleranceTestResource.class)
    @TestHTTPResource
    URL uri;

    @Test
    public void test() throws Exception {
        RestAssured
                .given().baseUri(uri.toString())
                .when().get()
                .then().body(is("2:Lucie"));

        RestAssured
                .given().baseUri(uri.toString() + "/retried")
                .when().get()
                .then().body(is("2:Lucie"));

        RestAssured
                .given().baseUri(uri.toString() + "/fallback")
                .when().get()
                .then().body(is("1:fallback"));

        RestAssured
                .given().baseUri(uri.toString() + "/hello")
                .when().get()
                .then().body(is("hello"));
    }
}
