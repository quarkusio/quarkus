package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals("1::4::Andy::false",
                engine.parse(
                        "{#let foo=1 bar='qute' baz=name.or('Andy') alpha=name.ifTruthy('true').or('false')}"
                                + "{#for i in foo}{i_count}{/for}::{bar.length}::{baz}::{alpha}"
                                + "{/let}")
                        .render());
    }

    @Test
    public void testDefaultValues() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("1", engine.parse("{#let foo?=1}{foo}{/let}").render());
        assertEquals("2", engine.parse("{#let foo?=1}{foo}{/let}").data("foo", 2).render());
        assertEquals("true::1::no", engine.parse("{#set foo?=true bar=1 baz?='yes'}{foo}::{bar}::{baz}{/set}")
                .data("bar", "42", "baz", "no").render());
    }

}
