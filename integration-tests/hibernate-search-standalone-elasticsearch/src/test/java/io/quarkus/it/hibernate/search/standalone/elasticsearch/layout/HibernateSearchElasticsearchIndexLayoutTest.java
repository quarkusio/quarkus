package io.quarkus.it.hibernate.search.standalone.elasticsearch.layout;

import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(HibernateSearchElasticsearchIndexLayoutTest.Profile.class)
public class HibernateSearchElasticsearchIndexLayoutTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.hibernate-search-standalone.elasticsearch.layout.strategy",
                    "bean:%s".formatted("CustomIndexLayoutStrategy"),
                    "test.index-layout.prefix", "this-is-a-test-value-");
        }
    }

    @Test
    public void testHibernateSearch() {
        // check the actual index name to see that the strategy worked:
        RestAssured.when().get("/test/layout-strategy/index-name").then()
                .statusCode(200)
                .body(is("LayoutEntity - this-is-a-test-value-layoutentity-read - this-is-a-test-value-layoutentity-write"));
    }
}
