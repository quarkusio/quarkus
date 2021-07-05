package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class WithSectionTest {

    @Test
    public void testWith() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#with map}{key}{/with}");
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> map = new HashMap<>();
        map.put("key", "val");
        data.put("map", map);
        assertEquals("val", template.render(data));
    }

    @Test
    public void testWithResultNotFound() {
        try {
            fail(Engine.builder().addDefaults().build().parse("{#with something}{key}{/with}").render());
        } catch (TemplateException expected) {
        }
    }

}
