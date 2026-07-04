package io.quarkus.spring.data.rest.crud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spring.data.rest.AbstractEntity;
import io.quarkus.test.QuarkusExtensionTest;

class DefaultListCrudResourceTest {
    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, Record.class, DefaultListCrudRecordsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldGet() {
        given().accept("application/json")
                .when().get("/default-list-crud-records/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first")));
    }

    @Test
    void shouldList() {
        given().accept("application/json")
                .when().get("/default-list-crud-records")
                .then().statusCode(200)
                .and().body("id", hasItems(1, 2))
                .and().body("name", hasItems("first", "second"));
    }
}
