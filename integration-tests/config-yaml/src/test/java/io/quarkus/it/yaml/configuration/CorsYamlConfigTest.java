package io.quarkus.it.yaml.configuration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.Header;
import io.vertx.core.http.HttpHeaders;

@QuarkusTest
public class CorsYamlConfigTest {

    private static final Header ORIGIN_HEADER = new Header(HttpHeaders.ORIGIN.toString(), "https://quarkus.io");

    @Test
    public void testStringValue() {
        checkAccessControlAllowedMethodsHeader("GET", is("GET"));
        checkAccessControlAllowedMethodsHeader("POST", is("POST"));
        checkAccessControlAllowedMethodsHeader("PUT", nullValue());
    }

    private void checkAccessControlAllowedMethodsHeader(String requestedMethod, Matcher<?> matcher) {
        given().header(ORIGIN_HEADER).header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD.toString(), requestedMethod)
                .get(CorsYamlConfigTestResource.PATH).then().statusCode(204)
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS.toString(), matcher);
    }
}
