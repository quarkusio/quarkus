package io.quarkus.it.hibernate.search.elasticsearch.devservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
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
                    // This version does not matter as long as it's supported by Hibernate Search:
                    // it won't be checked in this test anyway.
                    "quarkus.hibernate-search-orm.elasticsearch.version", "7.6",
                    "quarkus.hibernate-search-orm.elasticsearch.version-check.enabled", "false");
        }

        @Override
        public String getConfigProfile() {
            // Don't use %test properties;
            // that way, we can control whether quarkus.hibernate-search-orm.elasticsearch.hosts is set or not.
            // In this test, we DO set quarkus.hibernate-search-orm.elasticsearch.hosts (see above).
            return "someotherprofile";
        }

        @Override
        public List<TestResourceEntry> testResources() {
            // Enables injection of DevServicesContext
            return List.of(new TestResourceEntry(DevServicesContextSpy.class));
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
