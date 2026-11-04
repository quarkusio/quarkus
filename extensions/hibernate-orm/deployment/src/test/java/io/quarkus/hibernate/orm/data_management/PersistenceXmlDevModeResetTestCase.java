package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.MyEntityTestResource;
import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

/**
 * For a persistence unit defined in a persistence.xml file, Hibernate ORM picks up import.sql by default:
 * resetting the database from the Dev UI executes it once, like on startup.
 */
@Tag(TestTags.DEVMODE)
public class PersistenceXmlDevModeResetTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsManifestResource("META-INF/persistence-dev-mode-reset.xml", "persistence.xml")
                    .addAsResource(new StringAsset("quarkus.datasource.db-kind=h2\n"
                            + "quarkus.datasource.jdbc.url=jdbc:h2:mem:persistence-xml-dev-mode-reset;DB_CLOSE_DELAY=-1\n"),
                            "application.properties")
                    .addAsResource("import.sql"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue());

    public PersistenceXmlDevModeResetTestCase() {
        super("quarkus-datasource");
    }

    @Test
    public void importSqlExecutedOnceOnReset() throws Exception {
        RestAssured.when().get("/my-entity/count").then().body(is("2"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("3"));

        JsonNode success = super.executeJsonRPCMethod("reset", Map.of("ds", "<default>"));
        assertTrue(success.asBoolean());

        RestAssured.when().get("/my-entity/count").then().body(is("2"));
        // Executing the script a second time would have failed on the primary key
        assertThat(TEST.getLogRecords()).extracting(LogRecord::getMessage)
                .noneMatch(message -> message != null && (message.startsWith("Failed to")
                        || message.contains("GenerationTarget encountered exception accepting command")));
    }
}
