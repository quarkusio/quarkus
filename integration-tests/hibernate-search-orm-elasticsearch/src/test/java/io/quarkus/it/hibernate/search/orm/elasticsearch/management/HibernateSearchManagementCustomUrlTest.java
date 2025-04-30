package io.quarkus.it.hibernate.search.orm.elasticsearch.management;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpHeaders;

@QuarkusTest
@TestProfile(HibernateSearchManagementCustomUrlTest.Profile.class)
class HibernateSearchManagementCustomUrlTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.hibernate-search-orm.management.root-path", "custom-reindex");
        }
    }

    @Test
    void simple() {
        RestAssured.given()
                .queryParam("wait_for", "finished")
                .header(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                .post("http://localhost:9001/q/custom-reindex/reindex")
                .then().statusCode(200)
                .body(Matchers.stringContainsInOrder("Reindexing started", "Reindexing succeeded"));
    }
}
