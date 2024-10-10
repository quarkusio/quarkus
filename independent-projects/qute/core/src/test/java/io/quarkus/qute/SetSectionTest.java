package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class SetSectionTest {

    @Test
    public void testSet() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("NOT_FOUND - true:mix",
                engine.parse("{foo ?: 'NOT_FOUND'} - {#set foo=true bar='mix'}{foo}:{bar}{/}").render());
    }

    @Test
    public void testLet() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("NOT_FOUND:what?! - true:mix:what?!",
                engine.parse("{foo ?: 'NOT_FOUND'}:{baz} - {#let foo=true bar='mix'}{foo}:{bar}:{baz}{/}").data("baz", "what?!")
                        .render());
    }

    @Test
    public void testLiterals() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        engine.parse(
                "{#let foo=1 bar='qute' baz=name.or('Andy') alpha=name.ifTruthy('true').or('false')}"
                        + "{#for i in foo}{i_count}{/for}::{bar.length}::{baz}::{alpha}"
                        + "{/let}")
                        .instance().renderAsync().thenAccept(actual -> assertEquals("1::4::Andy::false", actual));
    }

    @Test
    public void testDefaultValues() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("1", engine.parse("{#let foo?=1}{foo}{/let}").render());
        assertEquals("2", engine.parse("{#let foo?=1}{foo}{/let}").data("foo", 2).render());
        assertEquals("true::1::no", engine.parse("{#set foo?=true bar=1 baz?='yes'}{foo}::{bar}::{baz}{/set}")
                .data("bar", "42", "baz", "no").render());
    }

    @Test
    public void testParameterOrigin() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("  {#let item = 1 foo=bar}{/let}");
        List<Expression> expressions = template.getExpressions();
        assertEquals(2, expressions.size());
        for (Expression expression : expressions) {
            if (expression.isLiteral()) {
                assertEquals(1, expression.getLiteralValue().getNow(false));
                assertEquals(1, expression.getOrigin().getLine());
                assertEquals(3, expression.getOrigin().getLineCharacterStart());
            } else {
                assertEquals("bar", expression.toOriginalString());
                assertEquals(1, expression.getOrigin().getLine());
                assertEquals(3, expression.getOrigin().getLineCharacterStart());
            }
        }
    }

    @Test
    public void testCompositeParams() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        assertEquals("1x2x::false",
                engine.parse(
                        "{#let foo=(baz + 1) bar=(name ? true : false)}"
                                + "{#for i in foo}{i_count}x{/for}::{bar}"
                                + "{/let}")
                        .data("baz", 1)
                        .render());
    }

    @Test
    public void testOptionalEndTag() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("true",
                engine.parse("{#let foo=true}{foo}").render());
        assertEquals("true  ?",
                engine.parse("{#let foo=true}{foo}  ?").render());
        assertEquals("true::1",
                engine.parse("{#let foo=true}{#let bar = 1}{foo}::{bar}").render());
        assertEquals("true",
                engine.parse("{#let foo=true}{#if baz}{foo}{/}").data("baz", true).render());
        assertEquals("true::null",
                engine.parse("{#if baz}{#let foo=true}{foo}{/if}::{foo ?: 'null'}").data("baz", true).render());
    }

}
