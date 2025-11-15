package io.quarkus.hibernate.search.orm.outboxpolling.test.configuration.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag("devmode")
public class HibernateSearchDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HibernateSearchOutboxPollingTestResource.class)
                    .addAsResource("application.properties", "application.properties"))
            .setLogRecordPredicate(r -> true);

    @Test
    public void smoke() {
        String[] schemaManagementStrategies = { "drop-and-create-and-drop", "drop-and-create" };

        RestAssured.when().put("/test/hibernate-search-outbox-polling/check-agents-running").then()
                .statusCode(200)
                .body(is("OK"));

        for (int i = 0; i < 3; i++) {
            int current = i;
            config.modifyResourceFile(
                    "application.properties",
                    s -> s.replace(
                            "quarkus.hibernate-search-orm.schema-management.strategy="
                                    + schemaManagementStrategies[current % 2],
                            "quarkus.hibernate-search-orm.schema-management.strategy="
                                    + schemaManagementStrategies[(current + 1) % 2]));

            RestAssured.when().put("/test/hibernate-search-outbox-polling/check-agents-running").then()
                    .statusCode(200)
                    .body(is("OK"));
        }

        assertThat(config.getLogRecords()).noneSatisfy(
                r -> assertThat(r.getMessage()).contains("Unable to shut down Hibernate Search"));
    }
}
