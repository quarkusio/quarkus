package io.quarkus.resteasy.reactive.links.deployment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.common.util.RestMediaType;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

public abstract class AbstractHalLinksTest {

    @Test
    void shouldGetHalLinksForCollections() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records")
                .thenReturn();

        assertThat(response.body().jsonPath().getList("_embedded.items.id")).containsOnly(1, 2);
        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/records");
    }

    @Test
    void shouldGetHalLinksForInstance() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/1")
                .thenReturn();

        assertThat(response.body().jsonPath().getString("_links.list.href")).endsWith("/records");
    }

    @Test
    void shouldGetHalLinksForIdAndPersistenceIdAndRestLinkId() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/with-id-and-persistence-id-and-rest-link-id/100")
                .thenReturn();

        assertThat(response.body()
                .jsonPath()
                .getString("_links.self.href")).endsWith("/records/with-id-and-persistence-id-and-rest-link-id/100");
    }

    @Test
    void shouldGetHalLinksForIdAndPersistenceId() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/with-id-and-persistence-id/10")
                .thenReturn();

        assertThat(response.body()
                .jsonPath()
                .getString("_links.self.href")).endsWith("/records/with-id-and-persistence-id/10");
    }

    @Test
    void shouldGetHalLinksForIdAndRestLinkId() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/with-id-and-rest-link-id/100")
                .thenReturn();

        assertThat(response.body()
                .jsonPath()
                .getString("_links.self.href")).endsWith("/records/with-id-and-rest-link-id/100");
    }

    @Test
    void shouldGetHalLinksForPersistenceIdAndRestLinkId() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/with-persistence-id-and-rest-link-id/100")
                .thenReturn();

        assertThat(response.body()
                .jsonPath()
                .getString("_links.self.href")).endsWith("/records/with-persistence-id-and-rest-link-id/100");
    }

    @Test
    void shouldGetHalLinksForPersistenceId() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/with-persistence-id/10")
                .thenReturn();

        assertThat(response.body().jsonPath().getString("_links.self.href")).endsWith("/records/with-persistence-id/10");
    }

    @Test
    void shouldGetHalLinksForRestLinkId() {
        Response response = given().accept(RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/with-rest-link-id/100")
                .thenReturn();

        assertThat(response.body().jsonPath().getString("_links.self.href")).endsWith("/records/with-rest-link-id/100");
        assertThat(response.body().jsonPath().getString("_links.self.title")).isEqualTo("The with rest link title");
        assertThat(response.body().jsonPath().getString("_links.self.type")).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void shouldIncludeAllFieldsFromLink() {
        Response response = given()
                .header(HttpHeaders.ACCEPT, RestMediaType.APPLICATION_HAL_JSON)
                .get("/records/with-rest-link-with-all-fields")
                .thenReturn();

        assertThat(response.body().jsonPath().getString("_links.all.href")).endsWith("/records/with-rest-link-id/100");
        assertThat(response.body().jsonPath().getString("_links.all.title")).isEqualTo("The title link");
        assertThat(response.body().jsonPath().getString("_links.all.type")).isEqualTo(MediaType.APPLICATION_JSON);
    }
}
