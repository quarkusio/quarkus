package io.quarkus.resteasy.links.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

public abstract class AbstractHalLinksTest {

    private static final String APPLICATION_HAL_JSON = "application/hal+json";

    @Test
    void shouldGetHalLinksForCollections() {
        Response response = given().accept(APPLICATION_HAL_JSON).get("/records").thenReturn();

        assertThat(response.body().jsonPath().getList("_embedded.items.id")).containsOnly(1, 2);
        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/records");
    }

    @Test
    void shouldGetHalLinksForInstance() {
        Response response = given().accept(APPLICATION_HAL_JSON).get("/records/first").thenReturn();

        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/records");
    }
}
