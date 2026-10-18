package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class EvalTest {

    @Test
    public void testEval() {
        Engine engine = Engine.builder().addDefaults().addSectionHelper(new EvalSectionHelper.Factory()).build();
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
                .isThrownBy(() -> Engine.builder().addDefaults().addSectionHelper(new EvalSectionHelper.Factory()).build()
                        .parse("{#eval name='Foo' /}"))
                .withMessageContainingAll("Parser error", "mandatory section parameters not declared");
    }

    @Test
    public void testInvalidTemplateContents() {
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> Engine.builder().addDefaults().addSectionHelper(new EvalSectionHelper.Factory()).build()
                        .parse("{#eval invalid /}").data("invalid", "{foo")
                        .render())
                .withMessageContainingAll("Parser error in the evaluated template", "unterminated expression");
    }

    @Test
    public void testVariantPropagated() {
        Engine engine = Engine.builder()
                .addSectionHelper(new EvalSectionHelper.Factory())
                .addDefaults()
                .addResultMapper(new HtmlEscaper(List.of("text/html")))
                .build();
        // The variant of the parent template should be propagated to the evaluated template
        // so that the HtmlEscaper is applied to the result
        assertEquals("&lt;p&gt;",
                engine.parse("{#eval '{foo}' /}", Variant.forContentType(Variant.TEXT_HTML)).data("foo", "<p>").render());
        assertEquals("&lt;p&gt;",
                engine.parse("{#eval foo /}", Variant.forContentType(Variant.TEXT_HTML)).data("foo", "{bar}", "bar", "<p>")
                        .render());
        // No variant - no escaping
        assertEquals("<p>",
                engine.parse("{#eval '{foo}' /}").data("foo", "<p>").render());
    }

    @Test
    public void testStrEvalNamespace() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addSectionHelper(new EvalSectionHelper.Factory())
                .addResultMapper(new HtmlEscaper(ImmutableList.of("text/html")))
                .addNamespaceResolver(new StrEvalNamespaceResolver())
                .build();
        assertEquals("Hello world!",
                engine.parse("{str:eval('Hello {name}!')}").data("name", "world").render());
        assertEquals("Hello world!",
                engine.parse("{str:eval(t1)}").data("t1", "Hello {name}!", "name", "world").render());
        // The variant of the parent template should be propagated to the evaluated template
        // so that the HtmlEscaper is applied to the result
        // Literal template
        assertEquals("&lt;p&gt;",
                engine.parse("{str:eval('{foo}')}", Variant.forContentType(Variant.TEXT_HTML)).data("foo", "<p>").render());
        // Non-literal template
        assertEquals("&lt;p&gt;",
                engine.parse("{str:eval(t1)}", Variant.forContentType(Variant.TEXT_HTML)).data("t1", "{foo}", "foo", "<p>")
                        .render());
        // No variant - no escaping
        // Note that the same literal is used as above but with a different variant, i.e. the variant must be a part of the cache key
        assertEquals("<p>",
                engine.parse("{str:eval('{foo}')}").data("foo", "<p>").render());
    }

}
