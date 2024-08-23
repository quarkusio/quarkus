package io.quarkus.it.hibernate.search.standalone.elasticsearch.devservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

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
@TestProfile(HibernateSearchElasticsearchDevServicesEnabledImplicitlyTest.Profile.class)
public class HibernateSearchElasticsearchDevServicesEnabledImplicitlyTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            // Don't use %test properties;
            // that way, we can control whether quarkus.hibernate-search-standalone.elasticsearch.hosts is set or not.
            // In this test, we do NOT set quarkus.hibernate-search-standalone.elasticsearch.hosts.
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
                .containsKey("quarkus.hibernate-search-standalone.elasticsearch.hosts");
        assertThat(context.devServicesProperties().get("quarkus.hibernate-search-standalone.elasticsearch.hosts"))
                .isNotEmpty()
                .isNotEqualTo("localhost:9200");
    }

    @Test
    public void testHibernateSearch() {
        RestAssured.when().get("/test/dev-services/hosts").then()
                .statusCode(200)
                .body(is(context.devServicesProperties().get("quarkus.hibernate-search-standalone.elasticsearch.hosts")));

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
