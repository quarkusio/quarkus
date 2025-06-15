package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.Response;

class PanacheEntityResourceHalDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addClasses(Project.class, ProjectResource.class).addAsResource("application.properties"));

    @Test
    void shouldHalNotBeSupported() {
        given().accept("application/hal+json").when().get("/group/projects/1").then().statusCode(406);
    }

    @Test
    void shouldNotContainLocationAndLinks() {
        Response response = given().accept("application/json").and().contentType("application/json").and()
                .body("{\"name\": \"projectname\"}").when().post("/group/projects").thenReturn();
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.header("Location")).isBlank();
        assertThat(response.getHeaders().getList("Link")).isEmpty();

        response = given().accept("application/json").when().get("/group/projects/projectname").thenReturn();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.header("Location")).isBlank();
        assertThat(response.getHeaders().getList("Link")).isEmpty();
    }
}
