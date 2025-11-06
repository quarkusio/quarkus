package io.quarkus.it.hibernate.search.orm.elasticsearch.devservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
@TestProfile(HibernateSearchElasticsearchWithElasticsearchJavaClientDevServicesTest.Profile.class)
public class HibernateSearchElasticsearchWithElasticsearchJavaClientDevServicesTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("quarkus.elasticsearch.devservices.enabled", "true");
            config.put("quarkus.hibernate-search-orm.elasticsearch.version", "9.1");
            return config;
        }

        @Override
        public String getConfigProfile() {
            // Don't use %test properties;
            // that way, we can control whether quarkus.hibernate-search-orm.elasticsearch.hosts is set or not.
            // In this test, we do NOT set quarkus.hibernate-search-orm.elasticsearch.hosts.
            return "someotherprofile";
        }
    }

    DevServicesContext context;

    @Inject
    RestClient client;

    @Test
    public void testDevServicesProperties() {
        assertThat(context.devServicesProperties())
                .containsKey("quarkus.hibernate-search-orm.elasticsearch.hosts");
    }

    @Test
    public void smoke() throws IOException {
        RestAssured.when().get("/test/dev-services/count").then()
                .statusCode(200)
                .body(is("0"));

        assertThat(client.performRequest(new Request("GET", "/")).getStatusLine().getStatusCode())
                .isEqualTo(200);

    }
}
