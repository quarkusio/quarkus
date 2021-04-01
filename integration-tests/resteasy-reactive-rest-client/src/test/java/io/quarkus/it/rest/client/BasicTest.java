package io.quarkus.it.rest.client;

import static java.util.stream.Collectors.counting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class BasicTest {

    @TestHTTPResource("/apples")
    String appleUrl;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void shouldWork() {
        List<Map> results = RestAssured.with().body(appleUrl).post("/call-client")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().body().jsonPath().getList(".", Map.class);
        assertThat(results).hasSize(9).allSatisfy(m -> {
            assertThat(m).containsOnlyKeys("cultivar");
        });
        Map<Object, Long> valueByCount = results.stream().collect(Collectors.groupingBy(m -> m.get("cultivar"), counting()));
        assertThat(valueByCount).containsOnly(entry("cortland", 3L), entry("cortland2", 3L), entry("cortland3", 3L));
    }
}
