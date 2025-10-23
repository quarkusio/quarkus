package io.quarkus.hibernate.orm.offline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Test that an application is running
 * even if the database is offline when the application starts
 * in DEV mode
 */
public class StartOfflineDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(GreetingResource.class)
                    .addAsResource("application-start-offline.properties", "application.properties"))
            .setLogRecordPredicate(record -> true);

    @Test
    public void testUnitSchemaManagementStrategyIsNone() {
        RestAssured.when().get("/hello").then()
                .statusCode(200)
                .body(is("DB is offline but application is running"));

        assertThat(runner.getLogRecords())
                .map(l -> l.getMessage())
                .doesNotContain("Failed to run post-boot validation");
    }

    @Path("/hello")
    public static class GreetingResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "DB is offline but application is running";
        }
    }

}
