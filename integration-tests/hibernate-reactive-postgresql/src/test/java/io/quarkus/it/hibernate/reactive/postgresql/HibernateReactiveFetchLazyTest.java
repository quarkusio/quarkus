package io.quarkus.it.hibernate.reactive.postgresql;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
@TestHTTPEndpoint(HibernateReactiveTestEndpointFetchLazy.class)
public class HibernateReactiveFetchLazyTest {

    @Test
    public void fetchAfterFindWithMutiny() {
        RestAssured.when()
                .post("/prepareDb")
                .then()
                .body(is("Neal Stephenson"));

        Response response = RestAssured.when()
                .get("/findBooksWithMutiny/567")
                .then()
                .extract().response();
        assertTitles(response, "Cryptonomicon", "Snow Crash");
    }

    private void assertTitles(Response response, String... expectedTitles) {
        List<Object> titles = response.jsonPath().getList("title").stream().sorted().collect(toList());
        assertIterableEquals(asList(expectedTitles), titles);
    }
}