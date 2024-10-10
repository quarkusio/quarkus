package io.quarkus.flyway.test;

import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

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
                            "quarkus.flyway.migrate-at-start=true\nquarkus.flyway.locations=db/update"), "application.properties"));

    @Test
    public void testGenerateMigrationFromHibernate() throws Exception {

        Map<String, Object> params = Map.of("ds", "<default>");
        JsonNode devuiresponse = super.executeJsonRPCMethod("update", params);

        Assertions.assertNotNull(devuiresponse);
        String type = devuiresponse.get("type").asText();
        Assertions.assertNotNull(type);
        Assertions.assertEquals("success", type);

    }

}
