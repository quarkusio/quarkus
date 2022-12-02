package io.quarkus.test.devconsole;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.devconsole.namedpu.MyNamedPuEntity;
import io.restassured.RestAssured;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleHibernateOrmActiveFalseAndNamedPuActiveTrueTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"
                            + "quarkus.datasource.\"nameddatasource\".db-kind=h2\n"
                            + "quarkus.datasource.\"nameddatasource\".jdbc.url=jdbc:h2:mem:test2\n"
                            // Hibernate ORM is inactive for the default PU
                            + "quarkus.hibernate-orm.active=false\n"
                            + "quarkus.hibernate-orm.datasource=<default>\n"
                            + "quarkus.hibernate-orm.packages=io.quarkus.test.devconsole\n"
                            // ... but it's (implicitly) active for a named PU
                            + "quarkus.hibernate-orm.\"namedpu\".datasource=nameddatasource\n"
                            + "quarkus.hibernate-orm.\"namedpu\".packages=io.quarkus.test.devconsole.namedpu\n"),
                    "application.properties")
                    .addClasses(MyEntity.class)
                    .addClasses(MyNamedPuEntity.class));

    @Test
    public void testPages() {
        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-orm/persistence-units")
                .then()
                .statusCode(200)
                .body(Matchers.not(Matchers.containsString("&lt;default&gt;")))
                .body(Matchers.containsString("Persistence Unit <i class=\"badge badge-info\">namedpu</i>"));

        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-orm/managed-entities")
                .then()
                .statusCode(200)
                .body(Matchers.not(Matchers.containsString(MyEntity.class.getName())))
                .body(Matchers.containsString(MyNamedPuEntity.class.getName()));

        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-orm/named-queries")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("No named queries were found."));
    }

}
