package io.quarkus.qute.deployment.jsonobject;

import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.json.JsonObject;

public class JsonObjectValueResolverTest {

    @RegisterExtension
    static final QuarkusExtensionTest quarkusApp = new QuarkusExtensionTest()
            .withApplicationRoot(
                    app -> app.addClass(foo.class)
                            .addAsResource(new StringAsset(
                                    "{tool.name} {tool.fieldNames} {tool.fields} {tool.size} {tool.empty} {tool.isEmpty} {tool.get('name')} {tool.containsKey('name')}"),
                                    "templates/JsonObjectValueResolverTest/foo.txt"));

    record foo(JsonObject tool) implements TemplateInstance {
    }

    @Test
    void testJsonObjectValueResolver() {
        HashMap<String, Object> toolMap = new HashMap<>();
        toolMap.put("name", "Roq");
        JsonObject jsonObject = new JsonObject(toolMap);
        String result = new foo(jsonObject).render();
        Assertions.assertThat(result).isEqualTo("Roq [name] [name] 1 false false Roq true");
    }
}
