package io.quarkus.flyway.test;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class FlywayDevModeUpdateFromHibernateTest extends DevUIJsonRPCTest {

    public FlywayDevModeUpdateFromHibernateTest() {
        super("io.quarkus.quarkus-flyway");
    }

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FlywayDevModeUpdateFromHibernateTest.class, Fruit.class)
                    .addAsResource(new StringAsset(
                            "    create sequence Fruit_SEQ start with 1 increment by 50;\n" +
                                    "\n" +
                                    "    create table Fruit (\n" +
                                    "        id integer not null,\n" +
                                    "        primary key (id)\n" +
                                    "    );"),
                            "db/update/V2.0.0__Quarkus.sql")
                    .addAsResource(new StringAsset(
                            "quarkus.flyway.migrate-at-start=true\nquarkus.flyway.locations=db/update"),
                            "application.properties"));

    @Test
    public void testGenerateMigrationFromHibernate() throws Exception {

        Map<String, Object> params = Map.of("ds", "<default>");
        JsonNode devuiresponse = super.executeJsonRPCMethod("update", params);

        Assertions.assertNotNull(devuiresponse);
        String type = devuiresponse.get("type").asText();
        Assertions.assertNotNull(type);
        Assertions.assertEquals("success", type);

        File migrationsDir = DevConsoleManager.getHotReplacementContext().getResourcesDir().get(0).resolve("db/update")
                .toFile();
        File[] newMigrations = migrationsDir.listFiles((dir, name) -> !name.equals("V2.0.0__Quarkus.sql"));
        Assertions.assertNotNull(newMigrations);
        Assertions.assertEquals(1, newMigrations.length);
        Assertions.assertTrue(newMigrations[0].getName().startsWith("V2."));

        String content = Files.readString(newMigrations[0].toPath());

        Assertions.assertEquals("\n" +
                "    alter table if exists Fruit \n" +
                "       add column name varchar(40);\n" +
                "\n" +
                "    alter table if exists Fruit \n" +
                "       drop constraint if exists UKqn1mp5t3oovyl0h02glapi2iv;\n" +
                "\n" +
                "    alter table if exists Fruit \n" +
                "       add constraint UKqn1mp5t3oovyl0h02glapi2iv unique (name);\n", content);
    }

}
