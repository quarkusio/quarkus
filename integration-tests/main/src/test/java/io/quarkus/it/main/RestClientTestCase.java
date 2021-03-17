package io.quarkus.it.main;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

@QuarkusTest
public class RestClientTestCase {

    public static final String HEADER_NAME = "header-name";

    @Test
    public void testMicroprofileClient() {
        RestAssured.when().get("/client/manual").then()
                .body(is("TEST"));
    }

    @Test
    public void testMicroprofileClientConfigKeyIntegration() {
        RestAssured.when().get("/client/annotation/configKey").then()
                .body(is("TEST"));
    }

    @Test
    public void testMicroprofileClientBaseUriConfigKeyIntegration() {
        RestAssured.when().get("/client/annotation/baseUriConfigKey").then()
                .body(is("TEST"));
    }

    @Test
    public void testMicroprofileClientCDIIntegration() {
        RestAssured.when().get("/client/cdi").then()
                .body(is("TEST"));
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    public void testEmojis() {
        RestAssured.when().get("/client/encoding")
                .then().body(is(
                        "\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00\uD83D\uDE00"));
    }

    @Test
    void testMicroprofileClientData() {
        JsonPath jsonPath = RestAssured.when().get("/client/manual/jackson").thenReturn().jsonPath();
        Assertions.assertEquals(jsonPath.getString("name"), "Stuart");
        Assertions.assertEquals(jsonPath.getString("value"), "A Value");
    }

    @Test
    void testMicroprofileClientDataCdi() {
        JsonPath jsonPath = RestAssured.when().get("/client/cdi/jackson").thenReturn().jsonPath();
        Assertions.assertEquals(jsonPath.getString("name"), "Stuart");
        Assertions.assertEquals(jsonPath.getString("value"), "A Value");
    }

    @Test
    void testMicroprofileAsyncRestClient() {
        RestAssured.when().get("/client/async/cdi").then().body(is("TEST"));
        JsonPath jsonPath = RestAssured.when().get("/client/async/cdi/jackson").thenReturn().jsonPath();
        Assertions.assertEquals(jsonPath.getString("name"), "Stuart");
        Assertions.assertEquals(jsonPath.getString("value"), "A Value");
    }

    @Test
    void testMicroprofileClientComplex() {
        JsonPath jsonPath = RestAssured.when().get("/client/manual/complex").thenReturn().jsonPath();
        List<Map<String, String>> components = jsonPath.getList("$");
        Assertions.assertEquals(components.size(), 1);
        Map<String, String> map = components.get(0);
        Assertions.assertEquals(map.get("value"), "component value");
    }

    @Test
    void testMicroprofileClientComplexCdi() {
        JsonPath jsonPath = RestAssured.when().get("/client/cdi/complex").thenReturn().jsonPath();
        List<Map<String, String>> components = jsonPath.getList("$");
        Assertions.assertEquals(components.size(), 1);
        Map<String, String> map = components.get(0);
        Assertions.assertEquals(map.get("value"), "component value");
    }

    @Test
    void testMicroprofileCdiClientHeaderPassing() {
        String headerValue = "some-not-at-all-random-header-value";
        RestAssured
                .given().header(HEADER_NAME, headerValue)
                .when().get("/client/manual/headers")
                .then().header("Content-Type", "application/json")
                .body(HEADER_NAME, equalTo(headerValue));
    }

    @Test
    void testMicroprofileClientHeaderPassing() {
        String headerValue = "some-not-at-all-random-header-value";
        RestAssured
                .given().header(HEADER_NAME, headerValue)
                .when().get("/client/cdi/headers")
                .then().header("Content-Type", "application/json")
                .body(HEADER_NAME, equalTo(headerValue));
    }

    @Test
    void testMicroprofileRestClientDefaultScope() {
        String responseWithSingletonScope = RestAssured
                .given()
                .when().get("/client/cdi/mp-rest-default-scope")
                .getBody().print();

        String responseWithDefaultScope = RestAssured
                .given()
                .when().get("/client/cdi/default-scope-on-interface")
                .getBody().print();

        Assertions.assertEquals("javax.inject.Singleton", responseWithSingletonScope);
        Assertions.assertEquals("javax.enterprise.context.Dependent", responseWithDefaultScope);
    }

    @Test
    void testJaxrsClientWithFilters() {
        given()
                .when().get("/client/jaxrs-client")
                .then()
                .statusCode(200)
                .body(containsString("hello"))
                .body(containsString("2020-02-13"));
    }

    @Test
    @Disabled("Disabled by default as it establishes external connections, uncomment when you want to test SSL support")
    public void testDegradedSslSupport() {
        RestAssured.when().get("/ssl").then()
                .statusCode(500)
                .body(containsString("SSL support"), containsString("disabled"));
    }

    @Test
    public void testIssue8795() {
        RestAssured.when().get("/client/publisher-client").then()
                .body(is("\n\ndata: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"
                        + "data: 75056-2\n\n"));
    }

    @Test
    public void testFaultTolerance() {
        RestAssured.when().get("/client/fault-tolerance").then()
                .body(is("Hello fallback!"));
    }
}
