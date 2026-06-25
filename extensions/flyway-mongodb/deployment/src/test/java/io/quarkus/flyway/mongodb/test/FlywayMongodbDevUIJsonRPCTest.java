package io.quarkus.flyway.mongodb.test;

import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

@ExtendWith(FlapdoodleMongodbExtension.class)
public class FlywayMongodbDevUIJsonRPCTest extends DevUIJsonRPCTest {

    public FlywayMongodbDevUIJsonRPCTest() {
        super("quarkus-flyway-mongodb");
    }

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("db.createCollection('devuitest');"),
                            "db/migration/V1__init.js")
                    .addAsResource(new StringAsset(
                            "quarkus.mongodb.connection-string=" + FlapdoodleMongodbExtension.MONGO_CONNECTION_STRING + "\n"
                                    + "quarkus.mongodb.database=devuitest\n"
                                    + "quarkus.flyway-mongodb.database=devuitest"),
                            "application.properties"));

    @Test
    public void testGetNumberOfClients() throws Exception {
        Integer count = super.executeJsonRPCMethod(Integer.class, "getNumberOfClients");
        Assertions.assertNotNull(count);
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testMigrate() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("migrate", Map.of("client", "<default>"));
        Assertions.assertNotNull(response);
        Assertions.assertEquals("success", response.get("type").asText());
    }

    @Test
    public void testClean() throws Exception {
        super.executeJsonRPCMethod("migrate", Map.of("client", "<default>"));
        JsonNode response = super.executeJsonRPCMethod("clean", Map.of("client", "<default>"));
        Assertions.assertNotNull(response);
        Assertions.assertEquals("success", response.get("type").asText());
    }

    @Test
    public void testGetClients() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("getClients");
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isArray());
        Assertions.assertEquals(1, response.size());
        JsonNode client = response.get(0);
        Assertions.assertEquals("<default>", client.get("name").asText());
    }
}
