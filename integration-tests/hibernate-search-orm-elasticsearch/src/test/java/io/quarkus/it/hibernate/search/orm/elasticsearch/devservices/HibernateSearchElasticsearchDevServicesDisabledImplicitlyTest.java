package io.quarkus.it.hibernate.search.orm.elasticsearch.devservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

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
@TestProfile(HibernateSearchElasticsearchDevServicesDisabledImplicitlyTest.Profile.class)
public class HibernateSearchElasticsearchDevServicesDisabledImplicitlyTest {
    private static final String EXPLICIT_HOSTS = "mycompany.com:4242";

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    // Make sure quarkus.hibernate-search-orm.elasticsearch.hosts is set,
                    // so that Quarkus detects disables Elasticsearch dev-services implicitly.
                    "quarkus.hibernate-search-orm.elasticsearch.hosts", EXPLICIT_HOSTS,
                    // Ensure we can work offline, because the host we set just above does not actually exist.
                    "quarkus.hibernate-search-orm.schema-management.strategy", "none",
                    "quarkus.hibernate-search-orm.elasticsearch.version-check.enabled", "false",
                    // When disabling the version check we need to set a more precise version
                    // than what we have in application.properties.
                    // But here it doesn't matter as we won't send a request to Elasticsearch anyway,
                    // so we're free to put anything.
                    // Just make sure to set something consistent with what we have in application.properties.
                    "quarkus.hibernate-search-orm.elasticsearch.version", "9.0");
        }

        @Override
        public String getConfigProfile() {
            // Don't use %test properties;
            // that way, we can control whether quarkus.hibernate-search-orm.elasticsearch.hosts is set or not.
            // In this test, we DO set quarkus.hibernate-search-orm.elasticsearch.hosts (see above).
            return "someotherprofile";
        }
    }

    DevServicesContext context;

    @Test
    public void testDevServicesProperties() {
        assertThat(context.devServicesProperties())
                .doesNotContainKey("quarkus.hibernate-search-orm.elasticsearch.hosts");
    }

    @Test
    public void testHibernateSearch() {
        RestAssured.when().get("/test/dev-services/hosts").then()
                .statusCode(200)
                .body(is(EXPLICIT_HOSTS));

        // We don't test Hibernate Search features (indexing, search) here,
        // because we're not sure that there is a host that Hibernate Search can talk to.
        // It's fine, though: we checked that Hibernate Search is configured as intended.
    }
}
