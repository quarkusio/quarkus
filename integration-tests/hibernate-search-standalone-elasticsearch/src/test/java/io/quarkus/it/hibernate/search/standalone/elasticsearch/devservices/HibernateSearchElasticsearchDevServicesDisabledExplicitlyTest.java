package io.quarkus.it.hibernate.search.standalone.elasticsearch.devservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

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
@TestProfile(HibernateSearchElasticsearchDevServicesDisabledExplicitlyTest.Profile.class)
public class HibernateSearchElasticsearchDevServicesDisabledExplicitlyTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>(); // Cannot use Map.of, we need nulls
            // Even if quarkus.hibernate-search-standalone.elasticsearch.hosts is not set,
            // Quarkus won't start Elasticsearch dev-services because of this explicit setting:
            config.put("quarkus.elasticsearch.devservices.enabled", "false");
            // Ensure we can work offline, because without dev-services,
            // we won't have an Elasticsearch instance to talk to.
            config.putAll(Map.of(
                    "quarkus.hibernate-search-standalone.schema-management.strategy", "none",
                    "quarkus.hibernate-search-standalone.elasticsearch.version-check.enabled", "false",
                    // When disabling the version check we need to set a more precise version
                    // than what we have in application.properties.
                    // But here it doesn't matter as we won't send a request to Elasticsearch anyway,
                    // so we're free to put anything.
                    // Just make sure to set something consistent with what we have in application.properties.
                    "quarkus.hibernate-search-standalone.elasticsearch.version", "9.0"));
            return config;
        }

        @Override
        public String getConfigProfile() {
            // Don't use %test properties;
            // that way, we can control whether quarkus.hibernate-search-standalone.elasticsearch.hosts is set or not.
            // In this test, we do NOT set quarkus.hibernate-search-standalone.elasticsearch.hosts.
            return "someotherprofile";
        }

    }

    DevServicesContext context;

    @Test
    public void testDevServicesProperties() {
        assertThat(context.devServicesProperties())
                .doesNotContainKey("quarkus.hibernate-search-standalone.elasticsearch.hosts");
    }

    @Test
    public void testHibernateSearch() {
        RestAssured.when().get("/test/dev-services/hosts").then()
                .statusCode(200)
                .body(is("localhost:9200")); // This is the default

        // We don't test Hibernate Search features (indexing, search) here,
        // because we're not sure that there is a host that Hibernate Search can talk to.
        // It's fine, though: we checked that Hibernate Search is configured as intended.
    }
}
