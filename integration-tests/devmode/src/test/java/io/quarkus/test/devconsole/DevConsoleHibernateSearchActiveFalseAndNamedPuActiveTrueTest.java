package io.quarkus.test.devconsole;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.devconsole.namedpu.MyNamedPuIndexedEntity;
import io.restassured.RestAssured;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleHibernateSearchActiveFalseAndNamedPuActiveTrueTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            + "quarkus.datasource.\"nameddatasource\".db-kind=h2\n"
                            + "quarkus.datasource.\"nameddatasource\".jdbc.url=jdbc:h2:mem:test2\n"
                            // Hibernate Search is inactive for the default PU
                            + "quarkus.hibernate-orm.datasource=<default>\n"
                            + "quarkus.hibernate-orm.packages=io.quarkus.test.devconsole\n"
                            + "quarkus.hibernate-search-orm.active=false\n"
                            + "quarkus.hibernate-search-orm.elasticsearch.version=7.10\n"
                            // ... but it's (implicitly) active for a named PU
                            + "quarkus.hibernate-orm.\"namedpu\".datasource=nameddatasource\n"
                            + "quarkus.hibernate-orm.\"namedpu\".packages=io.quarkus.test.devconsole.namedpu\n"
                            + "quarkus.hibernate-search-orm.\"namedpu\".elasticsearch.version=7.10\n"
                            // Start Hibernate Search offline for the named PU,
                            // because we don't have dev services except for the default PU
                            + "quarkus.hibernate-search-orm.\"namedpu\".schema-management.strategy=none\n"
                            + "quarkus.hibernate-search-orm.\"namedpu\".elasticsearch.version-check.enabled=false\n"),
                    "application.properties")
                    .addClasses(MyIndexedEntity.class)
                    .addClasses(MyNamedPuIndexedEntity.class));

    @Test
    public void testPages() {
        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-search-orm-elasticsearch/entity-types")
                .then()
                .statusCode(200)
                .body(Matchers.containsString(MyNamedPuIndexedEntity.class.getName()))
                .body(Matchers.not(Matchers.containsString(MyIndexedEntity.class.getName())));
    }

}
