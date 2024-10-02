package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class IncludeTest {

    @Test
    public void testInclude() {
        Engine engine = Engine.builder().addDefaults().build();

        engine.putTemplate("super", engine.parse("{this}: {#insert header}default header{/insert}"));
        assertEquals("HEADER: super header",
                engine.parse("{#include super}{#header}super header{/header}{/include}").render("HEADER"));
    }

    @Test
    public void testMultipleInserts() {
        Engine engine = Engine.builder().addDefaults().build();

        engine.putTemplate("super",
                engine.parse("{#insert header}default header{/insert} AND {#insert content}default content{/insert}"));

        Template template = engine
                .parse("{#include super}{#header}super header{/header}  {#content}super content{/content} {/include}");
        assertEquals("super header AND super content", template.render(null));
    }

    @Test
    public void testIncludeSimpleData() {
        Engine engine = Engine.builder().addDefaults().build();

        Map<String, String> data = new HashMap<>();
        data.put("name", "Al");
        data.put("price", "100");
        engine.putTemplate("detail", engine.parse("<strong>{name}</strong>:{price}"));
        assertEquals("<strong>Al</strong>:100",
                engine.parse("{#include detail/}").render(data));
    }

    @Test
    public void testOptionalBlockEndTags() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#insert header}header{/}:{#insert footer /}"));
        assertEquals("super header:super footer",
                engine.parse("{#include super}{#header}super header{#footer}super footer{/include}").render());
    }

    @Test
    public void testIncludeInLoop() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("{#insert snippet}empty{/insert}"));
        assertEquals("1.2.3.4.5.",
                engine.parse(
                        "{#for i in 5}{#include foo}{#snippet}{i_count}.{/snippet} this should be ingored {/include}{/for}")
                        .render());
    }

    @Test
    public void testIncludeInIf() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("{#insert snippet}empty{/insert}"));
        assertEquals("1",
                engine.parse("{#if true}{#include foo} {#snippet}1{/snippet} {/include}{/if}")
                        .render());
    }

    @Test
    public void testUserTagInsideInsert() {
        Engine engine = Engine.builder().addDefaults().addSectionHelper(new UserTagSectionHelper.Factory("hello", "hello"))
                .build();
        engine.putTemplate("hello", engine.parse("{name}"));
        engine.putTemplate("base", engine.parse("{#insert snippet}{/insert}"));
        assertEquals("foo",
                engine.parse("{#include base} {#snippet}{#hello name='foo'/}{/snippet} {/include}")
                        .render());
    }

    @Test
    public void testIncludeStandaloneLines() {
        Engine engine = Engine.builder().addDefaults().removeStandaloneLines(true).build();
        engine.putTemplate("super", engine.parse("{#insert header}\n"
                + "default header\n"
                + "{/insert}"));
        assertEquals("super header\n",
                engine.parse("{#include super}\n"
                        + "{#header}\n"
                        + "super header\n"
                        + "{/header}\n"
                        + "{/include}").render());
    }

    @Test
    public void testEmptyInclude() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("bar/fool.html", engine.parse("{foo} and {that}"));
        assertEquals("1 and true", engine.parse("{#include bar/fool.html that=true /}").data("foo", 1).render());
    }

    @Test
    public void testInsertParam() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#insert header}default header{/insert} and {#insert footer}{that}{/}"));
        Template foo = engine.parse("{#include 'super' that=foo}{#header}{that}{/}{/}");
        // foo, that
        assertEquals(2, foo.getExpressions().size());
        assertEquals("1 and 1", foo.data("foo", 1).render());
    }

    @Test
    public void testDefaultInsert() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<title>{#insert title}Default Title{/}</title>"
                + "</head>"
                + "<body>"
                + "  {#insert}No body!{/}"
                + "</body>"
                + "</html>"));
        assertEquals("<html>"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<title>My Title</title>"
                + "</head>"
                + "<body>"
                + "  Body 1!"
                + "</body>"
                + "</html>", engine.parse("{#include super}{#title}My Title{/title}Body {foo}!{/}").data("foo", 1).render());
    }

    @Test
    public void testAmbiguousInserts() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#insert header}default header{/insert}"));
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#include super}{#header}1{/}{#header}2{/}{/}"))
                .withMessage(
                        "Rendering error: multiple blocks define the content for the {#insert} section of name [header]")
                .hasFieldOrProperty("origin")
                .hasFieldOrProperty("code");
    }

    @Test
    public void testInsertInLoop() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#for i in 5}{#insert row}No row{/}{/for}"));
        assertEquals("1:2:3:4:5:", engine.parse("{#include super}{#row}{i}:{/row}{/}").render());
    }

    @Test
    public void testTagAndInsertConflict() {
        Engine engine = Engine.builder().addDefaults().addSectionHelper(new UserTagSectionHelper.Factory("row", "row")).build();
        engine.putTemplate("row", engine.parse("{foo}"));
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#insert}{/}\n{#insert row /}"))
                .withMessage(
                        "Parser error: {#insert} defined in the {#include} conflicts with an existing section/tag: row")
                .hasFieldOrProperty("origin")
                .hasFieldOrProperty("code");
    }

    @Test
    public void testIncludeNotFound() {
        Engine engine = Engine.builder().addDefaults().build();
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{#include super}{#header}super header{/header}{/include}", null, "foo.html")
                        .render())
                .withMessage(
                        "Rendering error in template [foo.html] line 1: included template [super] not found")
                .hasFieldOrProperty("origin")
                .hasFieldOrProperty("code");
    }

    @Test
    public void testIsolated() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("{val ?: 'bar'}"));
        assertEquals("bar", engine.parse("{#include foo _isolated /}").data("val", "baz").render());
    }

    @Test
    public void testFragment() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("---{#fragment bar}{val}{/fragment}---"));
        Template baz = engine.parse(
                "{#fragment nested}NESTED{/fragment} {#include foo$bar val=1 /} {#include baz$nested /}");
        engine.putTemplate("baz", baz);
        assertEquals("NESTED 1 NESTED", baz.render());
    }

    @Test
    public void testIgnoreFragments() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo$bar", engine.parse("{val}"));
        Template baz = engine.parse("{#include foo$bar val=1 _ignoreFragments=true /}");
        engine.putTemplate("baz", baz);
        assertEquals("1", baz.render());
    }

    @Test
    public void testInvalidFragment() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("foo"));
        TemplateException expected = assertThrows(TemplateException.class,
                () -> engine.parse("{#include foo$foo_and_bar /}", null, "bum.html").render());
        assertEquals(IncludeSectionHelper.Code.FRAGMENT_NOT_FOUND, expected.getCode());
        assertEquals(
                "Rendering error in template [bum.html] line 1: fragment [foo_and_bar] not found in the included template [foo]",
                expected.getMessage());

        expected = assertThrows(TemplateException.class,
                () -> engine.parse("{#include foo$foo-and_bar /}", null, "bum.html").render());
        assertEquals(IncludeSectionHelper.Code.INVALID_FRAGMENT_ID, expected.getCode());
        assertEquals(
                "Rendering error in template [bum.html] line 1: invalid fragment identifier [foo-and_bar]",
                expected.getMessage());
    }

    @Test
    public void testOptionalEndTag() {
        Engine engine = Engine.builder().addDefaults().build();

        engine.putTemplate("super", engine.parse("{#insert header}default header{/insert}::{#insert}{/}"));
        assertEquals("super header:: body",
                engine.parse("{#include super}{#header}super header{/header} body").render());
        assertEquals("super header:: 1",
                engine.parse("{#let foo = 1}{#include super}{#header}super header{/header} {foo}").render());
        assertEquals("default header:: 1",
                engine.parse("{#include super}{#let foo=1} {foo}").render());
    }

    @Test
    public void testIsolation() {
        Engine engine = Engine.builder()
                .addDefaults()
                .strictRendering(false)
                .build();

        Template foo = engine.parse("{name}");
        engine.putTemplate("foo", foo);
        assertEquals("Dorka", engine.parse("{#include foo /}").data("name", "Dorka").render());
        assertEquals("Dorka", engine.parse("{#include foo _unisolated /}").data("name", "Dorka").render());
        assertEquals("NOT_FOUND", engine.parse("{#include foo _isolated /}").data("name", "Dorka").render());
    }

    @Test
    public void testNestedMainBlocks() {
        Engine engine = Engine.builder()
                .addDefaults()
                .build();

        engine.putTemplate("root", engine.parse("""
                <html>
                   <body>{#insert /}</body>
                </html>
                                """));
        engine.putTemplate("auth", engine.parse("""
                {#include root}
                <div>
                   {#insert /}
                </div>
                {/include}
                                """));
        assertEquals("<html><body><div><form>LoginForm</form></div></body>"
                + "</html>", engine.parse("""
                        {#include auth}
                           <form>Login Form</form>
                        {/include}
                                    """).render().replaceAll("\\s+", ""));

        engine.putTemplate("next", engine.parse("""
                {#include auth}
                <foo>
                   {#insert /}
                </foo>
                {/include}
                                """));

        // 1. top -> push child rc#1 with extending block $default$
        // 2. next -> push child rc#2 with extending block $default$
        // 3. auth -> push child rc#3 with extending block $default$
        // 4. root -> eval {#insert}, looks up $default$ in rc#3
        // 5. auth -> eval {#insert}, looks up $default$ in rc#2
        // 6. next -> eval {#insert}, looks up $default$ in rc#1
        assertEquals("<html><body><div><foo><form>LoginForm</form></foo></div></body>"
                + "</html>", engine.parse("""
                        {#include next}
                           <form>Login Form</form>
                        {/include}
                                    """).render().replaceAll("\\s+", ""));
    }

    @Test
    public void testNestedBlocksWithSameName() {
        Engine engine = Engine.builder()
                .addDefaults()
                .build();

        engine.putTemplate("root", engine.parse("""
                <html>
                   <body>{#insert foo /}</body>
                </html>
                                """));
        engine.putTemplate("auth", engine.parse("""
                {#include root}
                {#foo}
                <div>
                   {#insert foo /}
                </div>
                {/foo}
                {/include}
                                """));
        assertEquals("<html><body><div><form>LoginForm</form></div></body>"
                + "</html>", engine.parse("""
                        {#include auth}
                           {#foo}
                           <form>Login Form</form>
                           {/foo}
                        {/include}
                                    """).render().replaceAll("\\s+", ""));

        engine.putTemplate("next", engine.parse("""
                {#include auth}
                {#foo}
                <foo>
                   {#insert foo /}
                </foo>
                {/foo}
                {/include}
                                """));

        assertEquals("<html><body><div><foo><form>LoginForm</form></foo></div></body>"
                + "</html>", engine.parse("""
                        {#include next}
                           {#foo}
                           <form>Login Form</form>
                           {/foo}
                        {/include}
                                    """).render().replaceAll("\\s+", ""));
    }

}
