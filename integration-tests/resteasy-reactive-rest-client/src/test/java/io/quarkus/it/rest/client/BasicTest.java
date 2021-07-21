package io.quarkus.it.rest.client;

import static java.util.stream.Collectors.counting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
public class BasicTest {

    @TestHTTPResource("/apples")
    String appleUrl;
    @TestHTTPResource()
    String baseUrl;

    @TestHTTPResource("/hello")
    String helloUrl;

    @Test
    public void shouldMakeTextRequest() {
        Response response = RestAssured.with().body(helloUrl).post("/call-hello-client");
        assertThat(response.asString()).isEqualTo("Hello, JohnJohn");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void shouldMakeJsonRequest() {
        List<Map> results = RestAssured.with().body(appleUrl).post("/call-client")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().jsonPath().getList(".", Map.class);
        assertThat(results).hasSize(9).allSatisfy(m -> {
            assertThat(m).containsOnlyKeys("cultivar");
        });
        Map<Object, Long> valueByCount = results.stream().collect(Collectors.groupingBy(m -> m.get("cultivar"), counting()));
        assertThat(valueByCount).containsOnly(entry("cortland", 3L), entry("lobo", 3L), entry("golden delicious", 3L));
    }

    @Test
    void shouldRetryOnFailure() {
        RestAssured.with().body(appleUrl).post("/call-client-retry")
                .then()
                .statusCode(200)
                .body(equalTo("4"));
    }

    @Test
    void shouldMapException() {
        RestAssured.with().body(baseUrl).post("/call-client-with-exception-mapper")
                .then()
                .statusCode(200);
    }

    @Test
    void shouldMapExceptionCdi() {
        RestAssured.with().body(baseUrl).post("/call-cdi-client-with-exception-mapper")
                .then()
                .statusCode(200);
    }
}
