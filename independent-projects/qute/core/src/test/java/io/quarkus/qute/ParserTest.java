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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                + "{@org.acme.Machine machine}"
                + "{foo.name}"
                + "{#for item in foo.items}"
                + "{item.name}{bar.name}"
                + "{/for}"
                + "{#each labels}"
                + "{it.name}"
                + "{/each}"
                + "{inject:bean.name}"
                + "{#each inject:bean.labels('foo')}"
                + "{it.value}"
                + "{/each}"
                + "{#set baz=foo.bar}"
                + "{baz.name}"
                + "{baz.getName(baz.age)}"
                + "{/set}"
                + "{#for foo in foos}"
                + "{foo.baz}"
                + "{/for}"
                + "{foo.call(labels,bar)}"
                + "{#when machine.status}{#is OK}..{#is NOK}{/when}");
        List<Expression> expressions = template.getExpressions();

        assertExpr(expressions, "foo.name", 2, "|org.acme.Foo|.name");

        Expression fooItems = find(expressions, "foo.items");
        assertExpr(expressions, "foo.items", 2, "|org.acme.Foo|.items<loop-element>");
        assertExpr(expressions, "item.name", 2, "item<loop#" + fooItems.getGeneratedId() + ">.name");
        assertExpr(expressions, "bar.name", 2, null);

        Expression labels = find(expressions, "labels");
        assertExpr(expressions, "labels", 1, "|java.util.List<org.acme.Label>|<loop-element>");
        assertExpr(expressions, "it.name", 2, "it<loop#" + labels.getGeneratedId() + ">.name");

        assertExpr(expressions, "inject:bean.name", 2, "inject:bean.name");

        Expression beanLabels = find(expressions, "inject:bean.labels('foo')");
        assertExpr(expressions, "inject:bean.labels('foo')", 2, "inject:bean.labels('foo')<loop-element>");
        assertExpr(expressions, "it.value", 2, "it<loop#" + beanLabels.getGeneratedId() + ">.value");

        Expression fooBar = find(expressions, "foo.bar");
        assertExpr(expressions, "foo.bar", 2, "|org.acme.Foo|.bar");
        assertExpr(expressions, "baz.name", 2, "baz<set#" + fooBar.getGeneratedId() + ">.name");
        assertExpr(expressions, "foo.baz", 2, null);
        assertExpr(expressions, "foo.call(labels,bar)", 2, "|org.acme.Foo|.call(labels,bar)");

        Expression machineStatusExpr = find(expressions, "machine.status");
        assertExpr(expressions, "OK", 1, "OK<when#" + machineStatusExpr.getGeneratedId() + ">");
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
        Origin fooItemsOrigin = find(template.getExpressions(), "foo.items").getOrigin();
        assertEquals(6, fooItemsOrigin.getLine());
        assertEquals(14, fooItemsOrigin.getLineCharacterStart());
        assertEquals(24, fooItemsOrigin.getLineCharacterEnd());
        Origin itemNameOrigin = find(template.getExpressions(), "item.name").getOrigin();
        assertEquals(8, itemNameOrigin.getLine());
        assertEquals(1, itemNameOrigin.getLineCharacterStart());
        assertEquals(11, itemNameOrigin.getLineCharacterEnd());
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
                "Parser error on line 1: unexpected non-text buffer at the end of the template - unterminated string literal: #if 'foo is null}{/}",
                1);
        assertParserError("{#if (foo || bar}{/}",
                "Parser error on line 1: unterminated string literal or composite parameter detected for [#if (foo || bar]", 1);
        assertParams("item.name == 'foo' and item.name is false", "item.name", "==", "'foo'", "and", "item.name", "is",
                "false");
        assertParams("(item.name == 'foo') and (item.name is false)", "(item.name == 'foo')", "and", "(item.name is false)");
        assertParams("(item.name != 'foo') || (item.name == false)", "(item.name != 'foo')", "||", "(item.name == false)");
    }

    @Test
    public void testWhitespace() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("Hello world", engine.parse("{#if true  }Hello {name }{/if  }").data("name", "world").render());
        assertEquals("Hello world", engine.parse("{#if true \n }Hello {name }{/if  }").data("name", "world").render());
        assertEquals("Hello world", engine.parse("{#if true \n || false}Hello {name }{/if  }").data("name", "world").render());
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
            assertEquals("Parser error on line 1: invalid identifier found {foo\nfoo}", expected.getMessage());
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

    @Test
    public void testGetExpressions() {
        Template template = Engine.builder().addDefaults().build()
                .parse("{foo}{#each items}{it.name}{#for foo in foos}{foo.name}{/for}{/each}");
        List<Expression> expressions = template.getExpressions();
        assertEquals("foo", expressions.get(0).toOriginalString());
        assertEquals("items", expressions.get(1).toOriginalString());
        assertEquals("it.name", expressions.get(2).toOriginalString());
        assertEquals("foos", expressions.get(3).toOriginalString());
        assertEquals("foo.name", expressions.get(4).toOriginalString());
    }

    @Test
    public void testParserHook() {
        Template template = Engine.builder().addDefaults().addParserHook(new ParserHook() {
            @Override
            public void beforeParsing(ParserHelper parserHelper) {
                parserHelper.addContentFilter(contents -> contents.replace("bard", "bar"));
                parserHelper.addContentFilter(contents -> contents.replace("${", "$\\{"));
            }
        }).build().parse("${foo}::{bard}");
        assertEquals("${foo}::true", template.data("bar", true).render());
    }

    @Test
    public void testStringLiteralWithTagEndDelimiter() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(ValueResolver.builder().applyToBaseClass(String.class)
                .applyToName("lines").resolveSync(ctx -> ctx.getBase().toString().split("\\n")).build()).build();
        Map<String, String> map = new HashMap<>();
        map.put("path", "/foo/bar");
        Template template = engine.parse("{#for line in map.get('{foo}').lines.orEmpty}{line}{/for}");
        assertEquals("", template.data("map", map).render());
        template = engine.parse("{#for line in map.get(foo).lines}{line}{/for}");
        assertEquals("/foo/bar", template.data("map", map, "foo", "path").render());

        assertParserError("{#if map.get(\"{foo})}Bye...{/if}",
                "Parser error on line 1: unexpected non-text buffer at the end of the template - unterminated string literal: #if map.get(\"{foo})}Bye...{/if}",
                1);
    }

    @Test
    public void testNestedHintValidation() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        Template loopLetLet = engine.parse("{@org.acme.Foo foo}"
                + "{#for item in foo.items}"
                + "{#let names=item.names}"
                + "{#let size=names.size}"
                + "{size}"
                + "{/let}"
                + "{/let}"
                + "{/for}");
        List<Expression> expressions = loopLetLet.getExpressions();
        assertExpr(expressions, "foo.items", 2, "|org.acme.Foo|.items<loop-element>");
        Expression itemNames = find(expressions, "item.names");
        assertExpr(expressions, "names.size", 2, "names<set#" + itemNames.getGeneratedId() + ">.size");
        Expression namesSize = find(expressions, "names.size");
        assertExpr(expressions, "size", 1, "size<set#" + namesSize.getGeneratedId() + ">");
        assertEquals("2", loopLetLet.data("foo", new Foo()).render());

        Template loopLetLoopLet = engine.parse("{@org.acme.Foo foo}"
                + "{#for item in foo.items}"
                + "{#let names=item.names}"
                + "{#for name in names}"
                + "{#let upperCase=name.toUpperCase}"
                + ":{upperCase.length}"
                + "{/let}"
                + "{/for}"
                + "{/let}"
                + "{/for}");
        expressions = loopLetLoopLet.getExpressions();
        assertExpr(expressions, "foo.items", 2, "|org.acme.Foo|.items<loop-element>");
        Expression fooItems = find(expressions, "foo.items");
        assertExpr(expressions, "item.names", 2, "item<loop#" + fooItems.getGeneratedId() + ">.names");
        itemNames = find(expressions, "item.names");
        // Note the 2 hints...
        assertExpr(expressions, "names", 1, "names<set#" + itemNames.getGeneratedId() + "><loop-element>");
        Expression names = find(expressions, "names");
        assertExpr(expressions, "name.toUpperCase", 2, "name<loop#" + names.getGeneratedId() + ">.toUpperCase");
        Expression nameToUpperCase = find(expressions, "name.toUpperCase");
        assertExpr(expressions, "upperCase.length", 2, "upperCase<set#" + nameToUpperCase.getGeneratedId() + ">.length");
        assertEquals(":3:5", loopLetLoopLet.data("foo", new Foo()).render());
    }

    @Test
    public void testInvalidNamespaceExpression() {
        assertParserError("{data: }",
                "Parser error on line 1: empty expression found {data:}", 1);
    }

    public static class Foo {

        public List<Item> getItems() {
            return Collections.singletonList(new Item());
        }

    }

    public static class Item {

        public List<String> getNames() {
            return Arrays.asList("foo", "bzink");
        }

    }

    private void assertParserError(String template, String message, int line) {
        Engine engine = Engine.builder().addDefaultSectionHelpers().build();
        try {
            engine.parse(template);
            fail("No parser error found");
        } catch (TemplateException expected) {
            assertNotNull(expected.getOrigin());
            assertEquals(line, expected.getOrigin().getLine(), "Wrong line");
            assertEquals(message, expected.getMessage());
        }
    }

    private void assertExpr(List<Expression> expressions, String value, int parts, String typeInfo) {
        Expression expr = find(expressions, value);
        assertEquals(parts, expr.getParts().size());
        assertEquals(typeInfo,
                expr.collectTypeInfo());
    }

    private Expression find(List<Expression> expressions, String val) {
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
