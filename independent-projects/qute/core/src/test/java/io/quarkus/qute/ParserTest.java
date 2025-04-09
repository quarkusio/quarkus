package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.TemplateException.Builder;
import io.quarkus.qute.TemplateLocator.TemplateLocation;
import io.quarkus.qute.TemplateNode.Origin;

public class ParserTest {

    @Test
    public void testSectionEndValidation() {
        assertParserError("{#if test}Hello {name}!{/for}", ParserError.SECTION_END_DOES_NOT_MATCH_START,
                "Parser error: section end tag [for] does not match the start tag [if]", 1);
        assertParserError("{#for i in items}{#if test}Hello {name}!{/for}", ParserError.SECTION_END_DOES_NOT_MATCH_START,
                "Parser error: section end tag [for] does not match the start tag [if]", 1);
    }

    @Test
    public void testSectionBlockEndValidation() {
        assertParserError("{#if test}Hello{#else}Hi{/elsa}{/if}", ParserError.SECTION_BLOCK_END_DOES_NOT_MATCH_START,
                "Parser error: section block end tag [elsa] does not match the start tag [else]", 1);
    }

    @Test
    public void testUnterminatedTag() {
        assertParserError("{#if test}Hello {name}", ParserError.UNTERMINATED_SECTION,
                "Parser error: unterminated section [if] detected", 1);
        assertParserError("{#if test}Hello {#for i in items}", ParserError.UNTERMINATED_SECTION,
                "Parser error: unterminated section [for] detected", 1);
    }

    @Test
    public void testSectionEndWithoutStart() {
        assertParserError("Hello {/}", ParserError.SECTION_START_NOT_FOUND,
                "Parser error: section start tag found for {/}", 1);
        assertParserError("{#if true}Bye...{/if} Hello {/if}", ParserError.SECTION_START_NOT_FOUND,
                "Parser error: section start tag found for {/if}", 1);
    }

    @Test
    public void testNonexistentHelper() {
        assertParserError("Hello!\n {#foo test/}", ParserError.NO_SECTION_HELPER_FOUND,
                "Parser error: no section helper found for {#foo test/}", 2);
    }

    @Test
    public void testNoSectionName() {
        assertParserError("Hello! {# foo=1 /}", ParserError.NO_SECTION_NAME,
                "Parser error: no section name declared for {# foo=1 /}", 1);
        assertParserError("Hello! {# for i in items}{/for}", ParserError.NO_SECTION_NAME,
                "Parser error: no section name declared for {# for i in items}", 1);
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
                + "{it_hasNext}"
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
                + "{#when machine.status}{#is OK}..{#is NOK}{/when}"
                + "{not_typesafe}");
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

        assertExpr(expressions, "it_hasNext", 1, "|java.lang.Boolean|<metadata>");
        assertExpr(expressions, "not_typesafe", 1, null);
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
        assertEquals(1, fooItemsOrigin.getLineCharacterStart());
        assertEquals(24, fooItemsOrigin.getLineCharacterEnd());
        Origin itemNameOrigin = find(template.getExpressions(), "item.name").getOrigin();
        assertEquals(8, itemNameOrigin.getLine());
        assertEquals(1, itemNameOrigin.getLineCharacterStart());
        assertEquals(11, itemNameOrigin.getLineCharacterEnd());
        ParameterDeclaration pd = template.getParameterDeclarations().get(0);
        assertEquals(1, pd.getOrigin().getLine());
        assertEquals("foo", pd.getKey());
        assertNull(pd.getDefaultValue());
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
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.getTemplate("foo.html"))
                .withMessage(
                        "Parser error in template [foo.html] line 1: mandatory section parameters not declared for {#if}: [condition]")
                .hasFieldOrProperty("origin");
    }

    @Test
    public void testSectionParameters() {
        assertParams("item.active || item.sold", "item.active", "||", "item.sold");
        assertParams("!(item.active || item.sold) || true", "!(item.active || item.sold)", "||", "true");
        assertParams("(item.active && (item.sold || false)) || user.loggedIn", "(item.active && (item.sold || false))", "||",
                "user.loggedIn");
        assertParams("this.get('name') is null", "this.get('name')", "is", "null");
        assertParserError("{#if 'foo is null}{/}", ParserError.UNTERMINATED_STRING_LITERAL,
                "Parser error: unexpected non-text buffer at the end of the template - unterminated string literal: #if 'foo is null}{/}",
                1);
        assertParserError("{#if (foo || bar}{/}", ParserError.UNTERMINATED_STRING_LITERAL_OR_COMPOSITE_PARAMETER,
                "Parser error: unterminated string literal or composite parameter detected for [#if (foo || bar]", 1);
        assertParams("item.name == 'foo' and item.name is false", "item.name", "==", "'foo'", "and", "item.name", "is",
                "false");
        assertParams("(item.name == 'foo') and (item.name is false)", "(item.name == 'foo')", "and", "(item.name is false)");
        assertParams("(item.name != 'foo') || (item.name == false)", "(item.name != 'foo')", "||", "(item.name == false)");
        assertParams("foo.codePointCount(0, foo.length) baz=bar", "foo.codePointCount(0, foo.length)", "baz=bar");
        assertParams("foo.codePointCount( 0 , foo.length( 1)) baz=bar", "foo.codePointCount( 0 , foo.length( 1))", "baz=bar");
        assertParams("item.name = 'foo' item.surname  = 'bar'", "item.name='foo'", "item.surname='bar'");
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
        assertThatExceptionOfType(Exception.class)
                .isThrownBy(() -> engine.parse("Hello {name} " + jsSnippet));

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
                + "{i_index}:\n"
                + "{/} "; // -> standalone
        assertEquals("\n0:\n1:\n2:\n3:\n4:\n", engine.parse(content).render());
        assertEquals("bar\n", engine.parse("{foo}\n").data("foo", "bar").render());
    }

    @Test
    public void testValidIdentifiers() {
        assertTrue(Identifiers.isValid("foo"));
        assertTrue(Identifiers.isValid("_foo"));
        assertTrue(Identifiers.isValid("foo$$bar"));
        assertTrue(Identifiers.isValid("1Foo_$"));
        assertTrue(Identifiers.isValid("1"));
        assertTrue(Identifiers.isValid("1?"));
        assertTrue(Identifiers.isValid("1:"));
        assertTrue(Identifiers.isValid("-foo"));
        assertTrue(Identifiers.isValid("foo["));
        assertTrue(Identifiers.isValid("foo^"));
        Engine engine = Engine.builder().addDefaults().build();
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> engine.parse("{foo\nfoo}"))
                .withMessage("Parser error: invalid identifier found [foo\nfoo]");
    }

    @Test
    public void testTextNodeCollapse() {
        Template template = Engine.builder().addDefaults().build().parse("Hello\nworld!{foo}next");
        List<TemplateNode> rootNodes = template.getNodes();
        assertEquals(3, rootNodes.size());
        assertEquals("Hello\nworld!", rootNodes.get(0).asText().getValue());
        assertTrue(rootNodes.get(1).isExpression());
        assertEquals("next", rootNodes.get(2).asText().getValue());
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
    public void testFindExpression() {
        Template template = Engine.builder().addDefaults().build()
                .parse("{foo}{#each items}{it.name}{#for foo in foos}\n{foo.name}{/for}{/each}");
        Expression fooExpr = template.findExpression(e -> e.toOriginalString().equals("foo"));
        assertNotNull(fooExpr);
        assertEquals(1, fooExpr.getOrigin().getLine());
        Expression itemsExpr = template.findExpression(e -> e.toOriginalString().equals("items"));
        assertNotNull(itemsExpr);
        Expression fooNameExpr = template.findExpression(e -> e.toOriginalString().equals("foo.name"));
        assertNotNull(fooNameExpr);
        assertEquals(2, fooNameExpr.getOrigin().getLine());
    }

    @Test
    public void testParserHook() {
        Template template = Engine.builder().addDefaults().addParserHook(new ParserHook() {
            @Override
            public void beforeParsing(ParserHelper parserHelper) {
                parserHelper.addContentFilter(contents -> contents.replace("bard", "bar"));
                parserHelper.addContentFilter(contents -> contents.replace("${", "$\\{"));
                parserHelper.addParameter("foo", String.class.getName());
            }
        }).build().parse("${foo}::{bard}");
        assertEquals("${foo}::true", template.data("bar", true).render());
        List<ParameterDeclaration> paramDeclarations = template.getParameterDeclarations();
        assertEquals(1, paramDeclarations.size());
        ParameterDeclaration pd = paramDeclarations.get(0);
        assertEquals("foo", pd.getKey());
        assertEquals("|java.lang.String|", pd.getTypeInfo());
        assertTrue(pd.getOrigin().isSynthetic());
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

        assertParserError("{#if map.get(\"{foo})}Bye...{/if}", ParserError.UNTERMINATED_STRING_LITERAL,
                "Parser error: unexpected non-text buffer at the end of the template - unterminated string literal: #if map.get(\"{foo})}Bye...{/if}",
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
        assertParserError("{data: }", ParserError.EMPTY_EXPRESSION,
                "Parser error: empty expression found {data:}", 1);
    }

    @Test
    public void testInvalidVirtualMethod() {
        assertParserError("{foo.baz()(}", ParserError.INVALID_VIRTUAL_METHOD,
                "Parser error: invalid virtual method in {foo.baz()(}", 1);
    }

    @Test
    public void testInvalidBracket() {
        assertParserError("{foo.baz[}", ParserError.INVALID_BRACKET_EXPRESSION,
                "Parser error: invalid bracket notation expression in {foo.baz[}", 1);
    }

    @Test
    public void testInvalidParamDeclaration() {
        assertParserError("{@com.foo }", ParserError.INVALID_PARAM_DECLARATION,
                "Parser error: invalid parameter declaration {@com.foo }", 1);
        assertParserError("{@ com.foo }", ParserError.INVALID_PARAM_DECLARATION,
                "Parser error: invalid parameter declaration {@ com.foo }", 1);
        assertParserError("{@com.foo.Bar bar baz}", ParserError.INVALID_PARAM_DECLARATION,
                "Parser error: invalid parameter declaration {@com.foo.Bar bar baz}", 1);
        assertParserError("{@}", ParserError.INVALID_PARAM_DECLARATION,
                "Parser error: invalid parameter declaration {@}", 1);
        assertParserError("{@\n}", ParserError.INVALID_PARAM_DECLARATION,
                "Parser error: invalid parameter declaration {@\n}", 1);
        assertParserError("{@com.foo.Bar<String baz}", ParserError.INVALID_PARAM_DECLARATION,
                "Parser error: invalid parameter declaration {@com.foo.Bar<String baz}", 1);

        Engine engine = Engine.builder().addDefaultSectionHelpers().build();
        Template template = engine.parse("{@com.foo.Bar<? extends org.acme.Baz, String> bar} {bar.name}");
        Expression bar = find(template.getExpressions(), "bar.name");
        assertEquals("|com.foo.Bar<org.acme.Baz, String>|", bar.getParts().get(0).getTypeInfo());
    }

    @Test
    public void testUserTagVirtualMethodParam() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("form", "form-template")).build();
        engine.putTemplate("form-template", engine.parse("{it}"));
        Template foo = engine.parse("{#form foo.codePointCount(0, foo.length) /}");
        assertEquals("3", foo.data("foo", "foo").render());
    }

    @Test
    public void testInvalidIdentifier() {
        assertParserError("{fo\to}", ParserError.INVALID_IDENTIFIER,
                "Parser error: invalid identifier found [fo\to]", 1);
    }

    @Test
    public void testMandatorySectionParas() {
        assertParserError("{#include /}", ParserError.MANDATORY_SECTION_PARAMS_MISSING,
                "Parser error: mandatory section parameters not declared for {#include /}: [template]", 1);
    }

    @Test
    public void testSectionParameterWithNestedSingleQuotationMark() {
        Engine engine = Engine.builder().addDefaults().build();
        assertSectionParams(engine, "{#let id=\"'Foo'\"}", Map.of("id", "\"'Foo'\""));
        assertSectionParams(engine, "{#let id=\"'Foo \"}", Map.of("id", "\"'Foo \""));
        assertSectionParams(engine, "{#let id=\"'Foo ' \"}", Map.of("id", "\"'Foo ' \""));
        assertSectionParams(engine, "{#let id=\"'Foo ' \" bar='baz'}", Map.of("id", "\"'Foo ' \"", "bar", "'baz'"));
        assertSectionParams(engine, "{#let my=bad id=(foo + 1) bar='baz'}",
                Map.of("my", "bad", "id", "(foo + 1)", "bar", "'baz'"));
        assertSectionParams(engine, "{#let id = 'Foo'}", Map.of("id", "'Foo'"));
        assertSectionParams(engine, "{#let id= 'Foo'}", Map.of("id", "'Foo'"));
        assertSectionParams(engine, "{#let my = (bad or not) id=1}", Map.of("my", "(bad or not)", "id", "1"));
        assertSectionParams(engine, "{#let my= (bad or not) id=1}", Map.of("my", "(bad or not)", "id", "1"));
    }

    @Test
    public void testVirtualMethodWithNestedLiteralSeparator() {
        Engine engine = Engine.builder().addDefaults().build();
        List<Part> parts = engine.parse("{foo('Bar \"!')}").findExpression(e -> true).getParts();
        assertVirtualMethodParam(parts.get(0), "foo", "Bar \"!");

        parts = engine.parse("{foo(\"Bar '!\")}").findExpression(e -> true).getParts();
        assertVirtualMethodParam(parts.get(0), "foo", "Bar '!");

        parts = engine.parse("{foo(\"Bar '!\").baz(1)}").findExpression(e -> true).getParts();
        assertVirtualMethodParam(parts.get(0), "foo", "Bar '!");
        assertVirtualMethodParam(parts.get(1), "baz", "1");

        parts = engine.parse("{str:builder('Qute').append(\"is '\").append(\"cool!\")}").findExpression(e -> true).getParts();
        assertVirtualMethodParam(parts.get(0), "builder", "Qute");
        assertVirtualMethodParam(parts.get(1), "append", "is '");
        assertVirtualMethodParam(parts.get(2), "append", "cool!");
    }

    private void assertVirtualMethodParam(Part part, String name, String literal) {
        assertTrue(part.isVirtualMethod());
        assertEquals(name, part.getName());
        assertTrue(part.asVirtualMethod().getParameters().get(0).isLiteral());
        assertEquals(literal, part.asVirtualMethod().getParameters().get(0).getLiteral().toString());
    }

    @Test
    public void testNonLiteralBracketNotation() {
        TemplateException e = assertThrows(TemplateException.class,
                () -> Engine.builder().addDefaults().build().parse("{foo[bar]}", null, "baz"));
        assertNotNull(e.getOrigin());
        assertEquals("Non-literal value [bar] used in bracket notation in expression {foo[bar]} in template [baz] line 1",
                e.getMessage());
    }

    private void assertSectionParams(Engine engine, String content, Map<String, String> expectedParams) {
        Template template = engine.parse(content);
        SectionNode node = template.findNodes(n -> n.isSection() && n.asSection().name.equals("let")).iterator().next()
                .asSection();
        Map<String, String> params = node.getBlocks().get(0).parameters;
        assertEquals(expectedParams, params);
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

    static void assertParserError(String template, ErrorCode code, String message, int line) {
        Engine engine = Engine.builder().addDefaultSectionHelpers().build();
        try {
            engine.parse(template);
            fail("No parser error found");
        } catch (TemplateException expected) {
            assertEquals(code, expected.getCode());
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
        class Dummy implements ErrorInitializer, WithOrigin {
            @Override
            public Origin getOrigin() {
                return null;
            }

            @Override
            public Builder error(String message) {
                return TemplateException.builder().message(message);
            }
        }
        Iterator<String> iter = Parser.splitSectionParams(content, new Dummy());
        List<String> params = new ArrayList<>();
        while (iter.hasNext()) {
            params.add(iter.next());
        }
        assertTrue(params.containsAll(Arrays.asList(expectedParams)),
                params + " should contain " + Arrays.toString(expectedParams));
    }

}
