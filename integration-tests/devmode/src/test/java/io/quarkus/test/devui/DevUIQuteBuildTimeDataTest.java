package io.quarkus.test.devui;

import java.util.Iterator;
import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIBuildTimeDataTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIQuteBuildTimeDataTest extends DevUIBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(new StringAsset(
                    "{hello}"),
                    "templates/hello.txt"));

    public DevUIQuteBuildTimeDataTest() {
        super("io.quarkus.quarkus-qute");
    }

    @Test
    public void testTemplates() throws Exception {
        List<String> allKeys = super.getAllKeys();
        Assertions.assertNotNull(allKeys);
        Assertions.assertTrue(allKeys.contains("extensionMethods"));
        Assertions.assertTrue(allKeys.contains("templates"));

        JsonNode templates = super.getBuildTimeData("templates");
        Assertions.assertNotNull(templates);
        Assertions.assertTrue(templates.isArray());

        Iterator<JsonNode> en = templates.elements();
        boolean exists = false;
        while (en.hasNext()) {
            JsonNode template = en.next();
            String path = template.get("path").asText();
            if (path.equals("hello.txt")) {
                exists = true;
                break;
            }
        }
        Assertions.assertTrue(exists);
    }

}
