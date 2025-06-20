package io.quarkus.it.hibernate.search.orm.elasticsearch;

import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(ElasticsearchClientTest.Profile.class)
public class ElasticsearchClientTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            // Don't use %test properties;
            // that way, we can control whether quarkus.hibernate-search-orm.elasticsearch.hosts is set or not.
            // In this test, we do NOT set quarkus.hibernate-search-orm.elasticsearch.hosts.
            return "someotherprofile";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.elasticsearch.devservices.port", "19201");
        }
    }

    @Test
    public void testConnection() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/connection").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testFullCycle() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/full-cycle").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testSniffer() throws Exception {
        RestAssured.when().get("/test/elasticsearch-client/sniffer").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
