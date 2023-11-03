package io.quarkus.spring.data.rest.crud;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

class DefaultCrudResourceTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AbstractEntity.class, Record.class, DefaultRecordsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));

    @Test
    void shouldGet() {
        given().accept("application/json")
                .when().get("/default-records/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first")));
    }

    @Test
    void shouldNotGetNonExistent() {
        given().accept("application/json")
                .when().get("/default-records/1000")
                .then().statusCode(404);
    }

    @Test
    void shouldGetHal() {
        given().accept("application/hal+json")
                .when().get("/default-records/1")
                .then().statusCode(200)
                .and().body("id", is(equalTo(1)))
                .and().body("name", is(equalTo("first")))
                .and().body("_links.add.href", endsWith("/default-records"))
                .and().body("_links.list.href", endsWith("/default-records"))
                .and().body("_links.self.href", endsWith("/default-records/1"))
                .and().body("_links.update.href", endsWith("/default-records/1"))
                .and().body("_links.remove.href", endsWith("/default-records/1"));
    }

    @Test
    void shouldNotGetNonExistentHal() {
        given().accept("application/hal+json")
                .when().get("/default-records/1000")
                .then().statusCode(404);
    }

    @Test
    void shouldList() {
        given().accept("application/json")
                .when().get("/default-records")
                .then().statusCode(200)
                .and().body("id", hasItems(1, 2))
                .and().body("name", hasItems("first", "second"));
    }

    @Test
    void shouldListHal() {
        given().accept("application/hal+json")
                .when().get("/default-records")
                .then().statusCode(200)
                .and().body("_embedded.default-records.id", hasItems(1, 2))
                .and().body("_embedded.default-records.name", hasItems("first", "second"))
                .and()
                .body("_embedded.default-records._links.add.href",
                        hasItems(endsWith("/default-records"), endsWith("/default-records")))
                .and()
                .body("_embedded.default-records._links.list.href",
                        hasItems(endsWith("/default-records"), endsWith("/default-records")))
                .and()
                .body("_embedded.default-records._links.self.href",
                        hasItems(endsWith("/default-records/1"), endsWith("/default-records/2")))
                .and()
                .body("_embedded.default-records._links.update.href",
                        hasItems(endsWith("/default-records/1"), endsWith("/default-records/2")))
                .and()
                .body("_embedded.default-records._links.remove.href",
                        hasItems(endsWith("/default-records/1"), endsWith("/default-records/2")))
                .and().body("_links.add.href", endsWith("/default-records"))
                .and().body("_links.list.href", endsWith("/default-records"));
    }

    @Test
    void shouldCreate() {
        Response response = given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-create\"}")
                .when().post("/default-records")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);

        String location = response.header("Location");
        int id = Integer.parseInt(location.substring(response.header("Location").lastIndexOf("/") + 1));
        JsonPath body = response.body().jsonPath();
        assertThat(body.getInt("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("test-create");

        given().accept("application/json")
                .when().get(location)
                .then().statusCode(200)
                .and().body("id", is(equalTo(id)))
                .and().body("name", is(equalTo("test-create")));
    }

    @Test
    void shouldCreateHal() {
        Response response = given().accept("application/hal+json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-create-hal\"}")
                .when().post("/default-records")
                .thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);

        String location = response.header("Location");
        int id = Integer.parseInt(location.substring(response.header("Location").lastIndexOf("/") + 1));
        JsonPath body = response.body().jsonPath();
        assertThat(body.getInt("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("test-create-hal");
        assertThat(body.getString("_links.add.href")).endsWith("/default-records");
        assertThat(body.getString("_links.list.href")).endsWith("/default-records");
        assertThat(body.getString("_links.self.href")).endsWith("/default-records/" + id);
        assertThat(body.getString("_links.update.href")).endsWith("/default-records/" + id);
        assertThat(body.getString("_links.remove.href")).endsWith("/default-records/" + id);

        given().accept("application/json")
                .when().get(location)
                .then().statusCode(200)
                .and().body("id", is(equalTo(id)))
                .and().body("name", is(equalTo("test-create-hal")));
    }

    @Test
    void shouldCreateAndUpdate() {
        Response createResponse = given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"101\", \"name\": \"test-update-create\"}")
                .when().put("/default-records/101")
                .thenReturn();
        assertThat(createResponse.statusCode()).isEqualTo(201);

        String location = createResponse.header("Location");
        int id = Integer.parseInt(location.substring(createResponse.header("Location").lastIndexOf("/") + 1));
        JsonPath body = createResponse.body().jsonPath();
        assertThat(body.getInt("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("test-update-create");

        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"" + id + "\", \"name\": \"test-update\"}")
                .when().put(location)
                .then()
                .statusCode(204);
        given().accept("application/json")
                .when().get(location)
                .then().statusCode(200)
                .and().body("id", is(equalTo(id)))
                .and().body("name", is(equalTo("test-update")));
    }

    @Test
    void shouldCreateAndUpdateHal() {
        Response createResponse = given().accept("application/hal+json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"102\", \"name\": \"test-update-create-hal\"}")
                .when().put("/default-records/102")
                .thenReturn();
        assertThat(createResponse.statusCode()).isEqualTo(201);

        String location = createResponse.header("Location");
        int id = Integer.parseInt(location.substring(createResponse.header("Location").lastIndexOf("/") + 1));
        JsonPath body = createResponse.body().jsonPath();
        assertThat(body.getInt("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("test-update-create-hal");
        assertThat(body.getString("_links.add.href")).endsWith("/default-records");
        assertThat(body.getString("_links.list.href")).endsWith("/default-records");
        assertThat(body.getString("_links.self.href")).endsWith("/default-records/" + id);
        assertThat(body.getString("_links.update.href")).endsWith("/default-records/" + id);
        assertThat(body.getString("_links.remove.href")).endsWith("/default-records/" + id);

        given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"id\": \"" + id + "\", \"name\": \"test-update-hal\"}")
                .when().put(location)
                .then()
                .statusCode(204);
        given().accept("application/json")
                .when().get(location)
                .then().statusCode(200)
                .and().body("id", is(equalTo(id)))
                .and().body("name", is(equalTo("test-update-hal")));
    }

    @Test
    void shouldCreateAndDelete() {
        Response createResponse = given().accept("application/json")
                .and().contentType("application/json")
                .and().body("{\"name\": \"test-delete\"}")
                .when().post("/default-records")
                .thenReturn();
        assertThat(createResponse.statusCode()).isEqualTo(201);

        String location = createResponse.header("Location");
        when().delete(location)
                .then().statusCode(204);
        when().get(location)
                .then().statusCode(404);
    }

    @Test
    void shouldNotDeleteNonExistent() {
        when().delete("/default-records/1000")
                .then().statusCode(404);
    }
}
