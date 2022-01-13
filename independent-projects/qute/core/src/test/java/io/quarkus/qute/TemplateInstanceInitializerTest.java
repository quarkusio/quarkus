package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TemplateInstanceInitializerTest {

    @Test
    public void testInitializer() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addTemplateInstanceInitializer(instance -> instance.data("foo", "bar").setAttribute("alpha", Boolean.TRUE))
                .build();

        Template hello = engine.parse("Hello {foo}!");
        TemplateInstance instance = hello.instance();
        assertEquals(Boolean.TRUE, instance.getAttribute("alpha"));
        assertEquals("Hello bar!", instance.render());
        instance.data("foo", "baz");
        assertEquals("Hello baz!", instance.render());
    }

}
