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
public class DevConsoleHibernateOrmSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(
                    new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:test\n"),
                    "application.properties")
                    .addClasses(MyEntity.class));

    @Test
    public void testLegacyPages() {
        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-orm/persistence-units")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Persistence Unit <i class=\"badge badge-info\">&lt;default&gt;</i>"));

        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-orm/managed-entities")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("io.quarkus.test.devconsole.MyEntity"));

        RestAssured.get("q/dev/io.quarkus.quarkus-hibernate-orm/named-queries")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("No named queries were found."));
    }

    @Test
    public void testPages() {
        // TODO #31970 restore tests of the page's content as we used to do for the old Dev UI

        RestAssured.get("q/dev-ui/hibernate-orm/persistence-units")
                .then()
                .statusCode(200);
        //      .body(Matchers.containsString("&lt;default&gt;"));

        RestAssured.get("q/dev-ui/hibernate-orm/entity-types")
                .then()
                .statusCode(200);
        //      .body(Matchers.containsString("io.quarkus.test.devconsole.MyEntity"));

        RestAssured.get("q/dev-ui/hibernate-orm/named-queries")
                .then()
                .statusCode(200);
        //      .body(Matchers.containsString("No named queries were found."));
    }

}
