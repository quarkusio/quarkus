package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class EvalTest {

    @Test
    public void testEval() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("Hello Foo!",
                engine.parse("{#eval 'Hello Foo!' /}").render());
        assertEquals("Hello Foo!",
                engine.parse("{#eval 'Hello Foo!'}ignored!{/eval}").render());
        assertEquals("Hello Lu!",
                engine.parse("{#eval foo /}").data("foo", "Hello {bar}!", "bar", "Lu").render());
        assertEquals("Hello Lu!",
                engine.parse("{#eval foo /}").data("foo", "Hello {#eval bar /}!", "bar", "Lu").render());
        assertEquals("Hello Foo and true!",
                engine.parse("{#eval name='Foo' template='Hello {name} and {bar}!' /}").data("bar", true).render());
        assertEquals("Hello Foo and true!",
                engine.parse("{#eval template name='Foo' /}").data("template", "Hello {name} and {bar}!", "bar", true)
                        .render());
    }

    @Test
    public void testTemplateParamNotSet() {
        try {
            Engine.builder().addDefaults().build().parse("{#eval name='Foo' /}");
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().contains("Parser error"));
            assertTrue(expected.getMessage().contains("mandatory section parameters not declared"));
        }
    }

    @Test
    public void testInvalidTemplateContents() {
        try {
            Engine.builder().addDefaults().build().parse("{#eval invalid /}").data("invalid", "{foo").render();
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().contains("Parser error in the evaluated template"));
        }
    }

}
