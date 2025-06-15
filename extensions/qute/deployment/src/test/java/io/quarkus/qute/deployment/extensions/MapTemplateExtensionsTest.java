package io.quarkus.qute.deployment.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class MapTemplateExtensionsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClass(Templates.class).addAsResource(
                    new StringAsset("{map.foo}::{map['bar']}::{map.containsKey('foo')}::{map.empty}::{map.get('foo')}"),
                    "templates/map.html"));

    @Test
    public void testMap() {
        assertEquals("1::5::true::false::1", Templates.map(Map.of("foo", 1, "bar", 5)).render());
    }

    @CheckedTemplate(basePath = "")
    public static class Templates {

        static native TemplateInstance map(Map<String, Object> map);
    }

}
