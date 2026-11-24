package io.quarkus.agroal.test;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

public class AgroalDevUITestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClass(DevModeResource.class)
                            .add(new StringAsset("quarkus.datasource.db-kind=h2\n" +
                                    "quarkus.datasource.username=USERNAME-NAMED\n" +
                                    "quarkus.datasource.jdbc.url=jdbc:h2:tcp://localhost/mem:testing\n" +
                                    "quarkus.datasource.jdbc.driver=org.h2.Driver\n" +
                                    "quarkus.datasource.dev-ui.allow-sql=true\n"), "application.properties");
                }
            });

    public AgroalDevUITestCase() {
        super("quarkus-agroal");
    }

    @Test
    public void testGetDataSources() throws Exception {
        JsonNode datasources = super.executeJsonRPCMethod("getDataSources");

        Assertions.assertNotNull(datasources);
        Assertions.assertTrue(datasources.isArray());
        Assertions.assertEquals(1, datasources.size());

        JsonNode datasource = datasources.get(0);
        Assertions.assertNotNull(datasource);

        JsonNode nameNode = datasource.get("name");
        Assertions.assertNotNull(nameNode);
        String name = nameNode.asText();
        Assertions.assertNotNull(name);
        Assertions.assertEquals(DEFAULT_DATASOURCE, name);
    }

    @Test
    public void testGetTables() throws Exception {
        JsonNode tables = super.executeJsonRPCMethod("getTables", Map.of("datasource", DEFAULT_DATASOURCE));
        Assertions.assertNotNull(tables);
        Assertions.assertTrue(tables.isArray());
    }

    @Test
    public void testGetInsertScript() throws Exception {
        JsonNode created = super.executeJsonRPCMethod("executeSQL", Map.of("datasource", DEFAULT_DATASOURCE,
                "sql", "CREATE SCHEMA IF NOT EXISTS EXPORT_TEST;"
                        + " CREATE TABLE IF NOT EXISTS EXPORT_TEST.FRUIT (ID INT PRIMARY KEY, NAME VARCHAR(40));"
                        + " MERGE INTO EXPORT_TEST.FRUIT VALUES (1, 'Cherry')",
                "pageNumber", 1, "pageSize", 10));
        Assertions.assertNotNull(created);

        JsonNode script = super.executeJsonRPCMethod("getInsertScript", Map.of("datasource", DEFAULT_DATASOURCE));
        Assertions.assertNotNull(script);
        Assertions.assertTrue(script.isString());
        Assertions.assertTrue(script.asString().contains("INSERT INTO EXPORT_TEST.FRUIT (ID, NAME) VALUES (1, 'Cherry');"),
                script::asString);
        Assertions.assertFalse(script.asString().contains("INFORMATION_SCHEMA"), script::asString);
    }

    private static final String DEFAULT_DATASOURCE = "<default>";
}
