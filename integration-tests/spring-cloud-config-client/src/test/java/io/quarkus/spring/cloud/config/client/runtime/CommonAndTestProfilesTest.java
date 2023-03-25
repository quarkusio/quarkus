package io.quarkus.spring.cloud.config.client.runtime;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(SpringCloudConfigServerResource.class)
@Tag("common")
@Tag("test")
public class CommonAndTestProfilesTest {
    @Test
    void config() {
        given()
                .get("/config/{name}", "greeting.message")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("hello from spring cloud config server"));
    }

    @Test
    void ordinal() {
        given()
                .get("/config/{name}", "foo")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("from foo development"))
                .body("sourceName", equalTo("https://github.com/spring-cloud-samples/config-repo/testapp-prod.yml"));

        given()
                .get("/config/{name}", "info.description")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("Sample"));
    }

    @Test
    void multiple() {
        given()
                .get("/config/{name}", "common")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("common"))
                .body("sourceName", equalTo("common"));
    }
}
