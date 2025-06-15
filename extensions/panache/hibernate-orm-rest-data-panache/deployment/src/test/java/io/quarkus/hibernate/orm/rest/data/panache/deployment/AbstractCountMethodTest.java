package io.quarkus.hibernate.orm.rest.data.panache.deployment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public abstract class AbstractCountMethodTest {

    @Test
    void shouldGetTotalNumberOfEntities() {
        given().get("/collections/count").then().statusCode(HttpStatus.SC_OK).and().body(is(equalTo("2")));
    }
}
