package io.quarkus.spring.cloud.config.client.runtime;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OidcBearerTokenTest.OidcProfile.class)
@Tag("common")
public class OidcBearerTokenTest {

    @Test
    void configIsFetchedWithTheBearerToken() {
        given()
                .get("/config/{name}", "greeting.message")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("hello from spring cloud config server"))
                .body("sourceName", equalTo("app-test-prod.yml"));
    }

    public static class OidcProfile implements QuarkusTestProfile {

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(OidcSpringCloudConfigServerResource.class));
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}
