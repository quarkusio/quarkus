package io.quarkus.it.hibernate.search.orm.opensearch.devservices;

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
@TestProfile(HibernateSearchOpenSearchDevServicesConfiguredExplicitlyTest.Profile.class)
public class HibernateSearchOpenSearchDevServicesConfiguredExplicitlyTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.elasticsearch.devservices.enabled", "true",
                    // This needs to be different from the default image, or the test makes no sense.
                    "quarkus.elasticsearch.devservices.image-name", "docker.io/opensearchproject/opensearch:2.12.0",
                    // This needs to match the version used just above,
                    // so that Hibernate Search itself will assert that we're using a custom version.
                    "quarkus.hibernate-search-orm.elasticsearch.version", "opensearch:2.12");
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

    @Test
    public void testDevServicesProperties() {
        assertThat(context.devServicesProperties())
                .containsKey("quarkus.hibernate-search-orm.elasticsearch.hosts");
        assertThat(context.devServicesProperties().get("quarkus.hibernate-search-orm.elasticsearch.hosts"))
                .isNotEmpty()
                .isNotEqualTo("localhost:9200");
    }

    @Test
    public void testHibernateSearch() {
        RestAssured.when().get("/test/dev-services/hosts").then()
                .statusCode(200)
                .body(is(context.devServicesProperties().get("quarkus.hibernate-search-orm.elasticsearch.hosts")));

        RestAssured.when().get("/test/dev-services/schema-management-strategy").then()
                .statusCode(200)
                // If the value is drop-and-create, this would indicate we're using the %test profile:
                // that would be a bug in this test (see the Profile class above).
                .body(is("drop-and-create-and-drop"));

        RestAssured.when().get("/test/dev-services/count").then()
                .statusCode(200)
                .body(is("0"));

        RestAssured.when().put("/test/dev-services/init-data").then()
                .statusCode(204);

        RestAssured.when().get("/test/dev-services/count").then()
                .statusCode(200)
                .body(is("1"));
    }
}
