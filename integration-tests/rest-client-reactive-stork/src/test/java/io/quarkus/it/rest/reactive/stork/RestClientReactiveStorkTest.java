package io.quarkus.it.rest.reactive.stork;

import static io.quarkus.it.rest.reactive.stork.FastWiremockServer.FAST_RESPONSE;
import static io.quarkus.it.rest.reactive.stork.SlowWiremockServer.SLOW_RESPONSE;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(SlowWiremockServer.class)
@QuarkusTestResource(FastWiremockServer.class)
public class RestClientReactiveStorkTest {

    @Test
    void shouldUseFasterService() {
        Set<String> responses = new HashSet<>();

        for (int i = 0; i < 2; i++) {
            Response response = when().get("/client");
            response.then().statusCode(200);
            responses.add(response.asString());
        }

        assertThat(responses).contains(FAST_RESPONSE, SLOW_RESPONSE);

        responses.clear();

        for (int i = 0; i < 3; i++) {
            Response response = when().get("/client");
            response.then().statusCode(200);
            responses.add(response.asString());
        }

        // after hitting the slow endpoint, we should only use the fast one:
        assertThat(responses).containsOnly(FAST_RESPONSE, FAST_RESPONSE, FAST_RESPONSE);
    }
}
