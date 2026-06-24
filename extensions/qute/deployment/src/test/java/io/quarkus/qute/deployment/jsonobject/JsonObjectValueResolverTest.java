package io.quarkus.qute.deployment.jsonobject;

import java.util.HashMap;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
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
                                    "templates/JsonObjectValueResolverTest/foo.txt")
                            .addAsResource(new StringAsset(
                                    "{tool.get(nonExistent)} {tool.containsKey(nonExistent)}"),
                                    "templates/notFound.txt")
                            .addAsResource(new StringAsset(
                                    "quarkus.qute.strict-rendering=false"),
                                    "application.properties"));

    record foo(JsonObject tool) implements TemplateInstance {
    }

    @Inject
    Template notFound;

    @Test
    void testJsonObjectValueResolver() {
        HashMap<String, Object> toolMap = new HashMap<>();
        toolMap.put("name", "Roq");
        JsonObject jsonObject = new JsonObject(toolMap);
        String result = new foo(jsonObject).render();
        Assertions.assertThat(result).isEqualTo("Roq [name] [name] 1 false false Roq true");
    }

    @Test
    void testJsonObjectNotFoundParam() {
        JsonObject jsonObject = new JsonObject(new HashMap<>());
        String result = notFound.render(jsonObject);
        Assertions.assertThat(result).isEqualTo("NOT_FOUND NOT_FOUND");
    }
}
