package io.quarkus.it.hibernate.search.orm.elasticsearch.coordination.outboxpolling;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag("devmode")
public class HibernateSearchDevModeFailingOrmTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HibernateSearchOutboxPollingTestResource.class, Person.class, OutboxPollingTestUtils.class)
                    .addAsResource("application-dev-mode.properties", "application.properties"))
            .setLogRecordPredicate(r -> true);

    static String APPLICATION_PROPERTIES;

    @BeforeAll
    static void beforeAll() throws Exception {
        APPLICATION_PROPERTIES = Files
                .readString(
                        Paths.get(HibernateSearchDevModeFailingOrmTest.class.getResource("/application-dev-mode.properties")
                                .toURI()));
    }

    @Test
    public void smoke() {
        RestAssured.when().put("/test/hibernate-search-outbox-polling/check-agents-running").then()
                .statusCode(200);

        // now add a property that will fail the datasource:
        config.modifyResourceFile(
                "application.properties",
                s -> APPLICATION_PROPERTIES + """
                        quarkus.datasource.jdbc.min-size=20
                        """);
        RestAssured.when().put("/test/hibernate-search-outbox-polling/check-agents-running").then()
                .statusCode(500);

        // add any change to get the shutdown of a failed app completed:
        config.modifyResourceFile("application.properties", s -> APPLICATION_PROPERTIES);

        RestAssured.when().put("/test/hibernate-search-outbox-polling/check-agents-running").then()
                .statusCode(200);

        // At this point we've tried starting the app 3 times: one initial, one failing, one successful live-reloads
        // Hence we expect the following things logged:
        // initial run:
        //  - profile activated (after a successful startup)
        //  - ORM message after a successful shutdown caused by a following live-reload (closing a PU)
        // first reload:
        //  - ORM message telling us that the PU closing won't happen as the PU failed to start
        // second reload:
        //  - profile activated (after a successful startup)
        //  - no ORM shutdown message, as that will happen after the test body finishes.
        assertThat(config.getLogRecords()).satisfiesOnlyOnce(
                r -> {
                    assertThat(r.getMessage()).contains("Closing Hibernate ORM persistence unit");
                    assertThat(r.getParameters()).containsExactly("<default>");
                });
        assertThat(config.getLogRecords()).satisfiesOnlyOnce(
                r -> {
                    assertThat(r.getMessage()).contains("Skipping Hibernate ORM persistence unit, that failed to start");
                    assertThat(r.getParameters()).containsExactly("<default>");
                });
        assertThat(config.getLogRecords().stream()
                .filter(r -> r.getMessage().contains("Profile%s %s activated. %s")))
                .hasSize(2);
    }
}
