package io.quarkus.test.devui;

import java.util.Iterator;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.BuildTimeDataResolver;
import io.quarkus.devui.tests.DevUITest;
import io.quarkus.devui.tests.Namespace;
import io.quarkus.test.QuarkusDevModeTest;

@DevUITest(@Namespace("io.quarkus.quarkus-qute"))
public class DevUIQuteBuildTimeDataTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addAsResource(new StringAsset(
                    "{hello}"),
                    "templates/hello.txt"));

    @Test
    public void testTemplates(BuildTimeDataResolver buildTimeDataResolver) throws Exception {
        final var response = buildTimeDataResolver
                .request()
                .send();
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.containsKey("extensionMethods"));
        Assertions.assertTrue(response.containsKey("templates"));

        JsonNode templates = response.get("templates");
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
