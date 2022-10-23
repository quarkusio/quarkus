package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NestedTemplateInstanceTest {

    @Test
    public void testNestedTemplateInstance() {
        Engine engine = Engine.builder().addDefaults().build();
        Template child = engine.parse("<b>{name}</b>");
        TemplateInstance childInstance = child.instance().data("name", "Qute");
        Template parent = engine.parse("Hello {child}!");
        TemplateInstance parentInstance = parent.instance().data("child", childInstance);
        assertEquals("Hello <b>Qute</b>!", parentInstance.render());
    }

}
