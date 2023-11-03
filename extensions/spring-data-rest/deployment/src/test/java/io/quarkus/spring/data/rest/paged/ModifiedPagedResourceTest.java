package io.quarkus.spring.data.rest.paged;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ModifiedPagedResourceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, Record.class, ModifiedRecordsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldGet() {
        given().accept("application/json")
                .when().get("/secret/records/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first")));
    }

    @Test
    void shouldGetHal() {
        given().accept("application/hal+json")
                .when().get("/secret/records/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first")))
                .and().body("_links.list.href", endsWith("/secret/records"))
                .and().body("_links.self.href", endsWith("/secret/records/1"))
                .and().body("_links.add.href", is(nullValue()))
                .and().body("_links.update.href", is(nullValue()))
                .and().body("_links.remove.href", is(nullValue()));
    }

    @Test
    void shouldList() {
        given().accept("application/json")
                .when().get("/secret/records")
                .then().statusCode(200)
                .and().body("id", hasItems(1, 2))
                .and().body("name", hasItems("first", "second"));
    }

    @Test
    void shouldListHal() {
        given().accept("application/hal+json")
                .when().get("/secret/records")
                .then().statusCode(200)
                .and().body("_embedded.secret-records.id", hasItems(1, 2))
                .and().body("_embedded.secret-records.name", hasItems("first", "second"))
                .and()
                .body("_embedded.secret-records._links.list.href",
                        hasItems(endsWith("/secret/records"), endsWith("/secret/records")))
                .and()
                .body("_embedded.secret-records._links.self.href",
                        hasItems(endsWith("/secret/records/1"), endsWith("/secret/records/2")))
                .and()
                .body("_embedded.secret-records._links.add.href", is(empty()))
                .and()
                .body("_embedded.secret-records._links.update.href", is(empty()))
                .and()
                .body("_embedded.secret-records._links.remove.href", is(empty()))
                .and().body("_links.list.href", endsWith("/secret/records"))
                .and().body("_links.add.href", is(nullValue()));
    }

    @Test
    void shouldNotCreate() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-create\"}")
                .when().post("/secret/records")
                .then().statusCode(405);
    }

    @Test
    void shouldNotUpdate() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-create\"}")
                .when().put("/secret/records/1")
                .then().statusCode(405);
    }

    @Test
    void shouldNotDelete() {
        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-create\"}")
                .when().delete("/secret/records/1")
                .then().statusCode(405);
    }
}
