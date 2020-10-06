package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.TemplateNode.Origin;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ParserTest {

    @Test
    public void testSectionEndValidation() {
        assertParserError("{#if test}Hello {name}!{/for}",
                "Parser error on line 1: section end tag [for] does not match the start tag [if]", 1);
    }

    @Test
    public void testUnterminatedTag() {
        assertParserError("{#if test}Hello {name}",
                "Parser error on line 1: unterminated section [if] detected", 1);
    }

    @Test
    public void testSectionEndWithoutStart() {
        assertParserError("Hello {/}",
                "Parser error on line 1: no section start tag found for {/}", 1);
        assertParserError("{#if true}Bye...{/if} Hello {/if}",
                "Parser error on line 1: no section start tag found for {/if}", 1);
    }

    @Test
    public void testNonexistentHelper() {
        assertParserError("Hello!\n {#foo test/}",
                "Parser error on line 2: no section helper found for {#foo test/}", 2);
    }

    @Test
    public void testIgnoreInvalidIdentifier() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("{\"foo\":\"bar\"} bar {'} baz ZX80",
                engine.parse("{\"foo\":\"bar\"} {_foo} {'} {1foo} {čip}").data("_foo", "bar").data("1foo", "baz")
                        .data("čip", "ZX80").render());
    }

    @Test
    public void testEscapingDelimiters() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("{foo} bar \\ignored {čip}",
                engine.parse("\\{foo\\} {foo} \\ignored \\{čip}").data("foo", "bar").render());
    }

    @Test
    public void testTypeInfos() {
        Engine engine = Engine.builder().addDefaultSectionHelpers()
                .build();
        Template template = engine.parse("{@org.acme.Foo foo}"
                + "{@java.util.List<org.acme.Label> labels}"
                + "{foo.name}"
                + "{#for item in foo.items}"
                + "{item.name}{bar.name}"
                + "{/for}"
                + "{#each labels}"
                + "{it.name}"
                + "{/each}"
                + "{inject:bean.name}"
                + "{#each inject:bean.labels}"
                + "{it.value}"
                + "{/each}"
                + "{#set baz=foo.bar}"
                + "{baz.name}"
                + "{/set}"
                + "{#for foo in foos}"
                + "{foo.baz}"
                + "{/for}"
                + "{foo.call(labels,bar)}");
        Set<Expression> expressions = template.getExpressions();

        assertExpr(expressions, "foo.name", 2, "|org.acme.Foo|.name");
        assertExpr(expressions, "foo.items", 2, "|org.acme.Foo|.items");
        assertExpr(expressions, "item.name", 2, "|org.acme.Foo|.items<for-element>.name");
        assertExpr(expressions, "bar.name", 2, null);
        assertExpr(expressions, "labels", 1, "|java.util.List<org.acme.Label>|");
        assertExpr(expressions, "it.name", 2, "|java.util.List<org.acme.Label>|<for-element>.name");
        assertExpr(expressions, "inject:bean.name", 2, "bean.name");
        assertExpr(expressions, "inject:bean.labels", 2, "bean.labels");
        assertExpr(expressions, "it.value", 2, "bean.labels<for-element>.value");
        assertExpr(expressions, "foo.bar", 2, "|org.acme.Foo|.bar");
        assertExpr(expressions, "baz.name", 2, "|org.acme.Foo|.bar.name");
        assertExpr(expressions, "foo.baz", 2, null);
        assertExpr(expressions, "foo.call(labels,bar)", 2, "|org.acme.Foo|.call(labels,bar)");
    }

    @Test
    public void testLines() {
        Engine engine = Engine.builder().addDefaultSectionHelpers()
                .build();
        Template template = engine.parse("{@org.acme.Foo foo}\n"
                + "<style type=\"text/css\">\n" +
                "body {\n" +
                "  font-family: sans-serif;\n" +
                "}\n"
                + "{#for item in foo.items}\n\n"
                + "{item.name}"
                + "{/}");
        assertEquals(6, find(template.getExpressions(), "foo.items").getOrigin().getLine());
        assertEquals(8, find(template.getExpressions(), "item.name").getOrigin().getLine());
    }

    @Test
    public void testNodeOrigin() {
        Engine engine = Engine.builder().addDefaultSectionHelpers()
                .build();
        Template template = engine.parse("12{foo}");
        Origin origin = find(template.getExpressions(), "foo").getOrigin();
        assertEquals(1, origin.getLine());
    }

    @Test
    public void testWithTemplateLocator() {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addLocator(id -> Optional.of(new TemplateLocation() {

            @Override
            public Reader read() {
                return new StringReader("{#if}");
            }

            @Override
            public Optional<Variant> getVariant() {
                return Optional.empty();
            }

        })).build();
        try {
            engine.getTemplate("foo.html");
            fail("No parser error found");
        } catch (TemplateException expected) {
            assertNotNull(expected.getOrigin());
            assertEquals(
                    "Parser error in template [foo.html] on line 1: mandatory section parameters not declared for {#if}: [Parameter [name=condition, defaultValue=null, optional=false]]",
                    expected.getMessage());
        }
    }

    @Test
    public void testSectionParameters() {
        assertParams("item.active || item.sold", "item.active", "||", "item.sold");
        assertParams("!(item.active || item.sold) || true", "!(item.active || item.sold)", "||", "true");
        assertParams("(item.active && (item.sold || false)) || user.loggedIn", "(item.active && (item.sold || false))", "||",
                "user.loggedIn");
        assertParams("this.get('name') is null", "this.get('name')", "is", "null");
        assertParserError("{#if 'foo is null}{/}",
                "Parser error on line 1: unterminated string literal or composite parameter detected for [#if 'foo is null]",
                1);
        assertParserError("{#if (foo || bar}{/}",
                "Parser error on line 1: unterminated string literal or composite parameter detected for [#if (foo || bar]", 1);
    }

    @Test
    public void testWhitespace() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("Hello world", engine.parse("{#if true  }Hello {name }{/if  }").data("name", "world").render());
        assertEquals("Hello world", engine.parse("Hello {name ?: 'world'  }").render());
    }

    @Test
    public void testCdata() {
        Engine engine = Engine.builder().addDefaults().build();
        String jsSnippet = "<script>const foo = function(){alert('bar');};</script>";
        try {
            engine.parse("Hello {name} " + jsSnippet);
            fail();
        } catch (Exception expected) {
        }
        assertEquals("Hello world <script>const foo = function(){alert('bar');};</script>", engine.parse("Hello {name} {["
                + jsSnippet
                + "]}").data("name", "world").render());
        assertEquals("Hello world <strong>", engine.parse("Hello {name} {[<strong>]}").data("name", "world").render());
        assertEquals("Hello world <script>const foo = function(){alert('bar');};</script>", engine.parse("Hello {name} {|"
                + jsSnippet
                + "|}").data("name", "world").render());
        assertEquals("Hello world <strong>", engine.parse("Hello {name} {|<strong>|}").data("name", "world").render());
        assertEquals("Hello {name} world", engine.parse("Hello{| {name} |}{name}").data("name", "world").render());
    }

    @Test
    public void testRemoveStandaloneLines() {
        Engine engine = Engine.builder().addDefaults().removeStandaloneLines(true).build();
        String content = "{@java.lang.String foo}\n" // -> standalone
                + "\n"
                + " {! My comment !} \n"
                + "  {#for i in 5}\n" // -> standalone
                + "{index}:\n"
                + "{/} "; // -> standalone
        assertEquals("\n0:\n1:\n2:\n3:\n4:\n", engine.parse(content).render());
        assertEquals("bar\n", engine.parse("{foo}\n").data("foo", "bar").render());
    }

    @Test
    public void testValidIdentifiers() {
        assertTrue(Parser.isValidIdentifier("foo"));
        assertTrue(Parser.isValidIdentifier("_foo"));
        assertTrue(Parser.isValidIdentifier("foo$$bar"));
        assertTrue(Parser.isValidIdentifier("1Foo_$"));
        assertTrue(Parser.isValidIdentifier("1"));
        assertTrue(Parser.isValidIdentifier("1?"));
        assertTrue(Parser.isValidIdentifier("1:"));
        assertTrue(Parser.isValidIdentifier("-foo"));
        assertTrue(Parser.isValidIdentifier("foo["));
        assertTrue(Parser.isValidIdentifier("foo^"));
        Engine engine = Engine.builder().addDefaults().build();
        try {
            engine.parse("{foo\nfoo}");
            fail();
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("Invalid identifier found"), expected.toString());
        }
    }

    @Test
    public void testTextNodeCollapse() {
        TemplateImpl template = (TemplateImpl) Engine.builder().addDefaults().build().parse("Hello\nworld!{foo}next");
        List<TemplateNode> rootNodes = template.root.blocks.get(0).nodes;
        assertEquals(3, rootNodes.size());
        assertEquals("Hello\nworld!", ((TextNode) rootNodes.get(0)).getValue());
        assertEquals(1, ((ExpressionNode) rootNodes.get(1)).getExpressions().size());
        assertEquals("next", ((TextNode) rootNodes.get(2)).getValue());
    }

    private void assertParserError(String template, String message, int line) {
        Engine engine = Engine.builder().addDefaultSectionHelpers().build();
        try {
            engine.parse(template);
            fail("No parser error found");
        } catch (TemplateException expected) {
            assertNotNull(expected.getOrigin());
            assertEquals(line, expected.getOrigin().getLine(), "Wrong line");
            assertEquals(message,
                    expected.getMessage());
        }
    }

    private void assertExpr(Set<Expression> expressions, String value, int parts, String typeInfo) {
        Expression expr = find(expressions, value);
        assertEquals(parts, expr.getParts().size());
        assertEquals(typeInfo,
                expr.collectTypeInfo());
    }

    private Expression find(Set<Expression> expressions, String val) {
        return expressions.stream().filter(e -> e.toOriginalString().equals(val)).findAny().get();
    }

    private void assertParams(String content, String... expectedParams) {
        Iterator<String> iter = Parser.splitSectionParams(content, s -> new RuntimeException(s));
        List<String> params = new ArrayList<>();
        while (iter.hasNext()) {
            params.add(iter.next());
        }
        assertTrue(params.containsAll(Arrays.asList(expectedParams)),
                params + " should contain " + Arrays.toString(expectedParams));
    }

}
