package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

public class ErrorPropagationTest {

    @Test
    public void testErrorPropagation() {
        Engine engine = Engine.builder()
                .strictRendering(true)
                .addSectionHelper("foo", new FooSectionHelper.FooSectionHelperFactory())
                .addDefaults()
                .build();

        TemplateException e = assertThrows(TemplateException.class,
                () -> engine.parse("{#let unused='var'}{bazbaz.size}{/let}").data("bazbaz", null).render());
        assertEquals("Rendering error: Property \"size\" not found on the base object \"null\" in expression {bazbaz.size}",
                e.getMessage());
        e = assertThrows(TemplateException.class,
                () -> engine.parse("{#for i in 10}{bazbaz.size}{/for}").data("bazbaz", null).render());
        assertEquals("Rendering error: Property \"size\" not found on the base object \"null\" in expression {bazbaz.size}",
                e.getMessage());
        e = assertThrows(TemplateException.class,
                () -> engine.parse("{#let unused='var'}{#if true}{bazbaz.size}{/if}{/let}").data("bazbaz", null).render());
        assertEquals("Rendering error: Property \"size\" not found on the base object \"null\" in expression {bazbaz.size}",
                e.getMessage());

        e = assertThrows(TemplateException.class,
                () -> engine.parse("{#let unused='var'}{#foo /}{/let}").render());
        assertTrue(e.getCause() instanceof NullPointerException);
        e = assertThrows(TemplateException.class,
                () -> engine.parse("{#for i in 10}{#foo /}{/for}").render());
        assertTrue(e.getCause() instanceof NullPointerException);
        e = assertThrows(TemplateException.class,
                () -> engine.parse("{#if true}{#foo /}{/if}").render());
        assertTrue(e.getCause() instanceof NullPointerException);
        e = assertThrows(TemplateException.class,
                () -> engine.parse("{#for i in 10}{#let unused='var'}{#if true}{#foo /}{/if}{/let}{/for}").render());
        assertTrue(e.getCause() instanceof NullPointerException);
        e = assertThrows(TemplateException.class,
                () -> engine.parse("{#let unused='var'}{#eval '{#if true}{#foo /}{/if}' /}{/let}").render());
        assertTrue(e.getCause() instanceof NullPointerException);
    }

    public static class FooSectionHelper implements SectionHelper {

        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            throw new NullPointerException();
        }

        public static class FooSectionHelperFactory implements SectionHelperFactory<FooSectionHelper> {

            @Override
            public FooSectionHelper initialize(SectionInitContext context) {
                return new FooSectionHelper();
            }

        }

    }

}
