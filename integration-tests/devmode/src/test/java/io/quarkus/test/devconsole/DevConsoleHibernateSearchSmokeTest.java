package io.quarkus.test.devconsole;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleHibernateSearchSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version=7.10\n"
                            // Start offline, we don't have an Elasticsearch cluster here
                            + "quarkus.hibernate-search-orm.schema-management.strategy=none\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version-check.enabled=false\n"),
                    "application.properties")
                    .addClasses(MyIndexedEntity.class));

    @Test
    public void testPages() {
        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-search-orm-elasticsearch/entity-types")
                .then()
                .statusCode(200)
                .body(Matchers.containsString(MyIndexedEntity.class.getName()));
    }

}
