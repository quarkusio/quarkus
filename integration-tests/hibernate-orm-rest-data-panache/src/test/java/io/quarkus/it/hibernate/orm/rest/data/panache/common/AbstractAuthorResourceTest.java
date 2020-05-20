package io.quarkus.it.hibernate.orm.rest.data.panache.common;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class AbstractAuthorResourceTest {

    private static final String APPLICATION_HAL_JSON = "application/hal+json";

    private static final AuthorDto DOSTOEVSKY = new AuthorDto(1L, "Fyodor Dostoevsky", LocalDate.of(1821, 11, 11));

    private static final AuthorDto ORWELL = new AuthorDto(2L, "George Orwell", LocalDate.of(1903, 06, 25));

    protected abstract String getResourceName();

    @BeforeEach
    void setUp() {
        RestAssured.basePath = getResourceName();
    }

    @AfterEach
    void tearDown() {
        RestAssured.basePath = "";
    }

    @Test
    void shouldGetOne() {
        when().get(DOSTOEVSKY.id.toString())
                .then().statusCode(200)
                .and().body("name", is(equalTo(DOSTOEVSKY.name)))
                .and().body("dob", is(equalTo(DOSTOEVSKY.dobAsString())));
    }

    @Test
    void shouldGetAll() {
        when().get()
                .then().statusCode(200)
                .and().body("name[0]", is(equalTo(DOSTOEVSKY.name)))
                .and().body("dob[0]", is(equalTo(DOSTOEVSKY.dobAsString())))
                .and().body("name[1]", is(equalTo(ORWELL.name)))
                .and().body("dob[1]", is(equalTo(ORWELL.dobAsString())));
    }

    @Test
    void halGetShouldNotBeExposed() {
        given().accept(APPLICATION_HAL_JSON)
                .when().get(DOSTOEVSKY.id.toString())
                .then().statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
        given().accept(APPLICATION_HAL_JSON)
                .when().get()
                .then().statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    @Test
    void postShouldNotBeExposed() {
        given().contentType(APPLICATION_JSON)
                .and().body(ORWELL)
                .when().post()
                .then().statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    void putShouldNotBeExposed() {
        given().contentType(APPLICATION_JSON)
                .and().body(ORWELL)
                .when().post(DOSTOEVSKY.id.toString())
                .then().statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }

    @Test
    void deleteShouldNotBeExposed() {
        when().delete(DOSTOEVSKY.id.toString())
                .then().statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
    }
}
