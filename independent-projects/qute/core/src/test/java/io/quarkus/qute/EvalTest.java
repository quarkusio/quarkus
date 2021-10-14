package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> Engine.builder().addDefaults().build().parse("{#eval name='Foo' /}"))
                .withMessageContainingAll("Parser error", "mandatory section parameters not declared");
    }

    @Test
    public void testInvalidTemplateContents() {
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> Engine.builder().addDefaults().build().parse("{#eval invalid /}").data("invalid", "{foo")
                        .render())
                .withMessageContaining("Parser error in the evaluated template");
    }

}
