package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.IfSectionHelper.Operator;

public class IfSectionTest {

    @Test
    public void tesIfElse() {
        Engine engine = Engine.builder().addDefaults().build();

        Template template = engine.parse("{#if isActive}ACTIVE{#else}INACTIVE{/if}");
        Map<String, Boolean> data = new HashMap<>();
        data.put("isActive", Boolean.FALSE);
        assertEquals("INACTIVE", template.render(data));

        template = engine.parse("{#if isActive}ACTIVE{#else if valid}VALID{#else}NULL{/if}");
        data.put("valid", Boolean.TRUE);
        assertEquals("VALID", template.render(data));
    }

    @Test
    public void testIfOperator() {
        Engine engine = Engine.builder().addDefaults().build();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "foo");
        data.put("foo", "foo");
        data.put("one", "1");
        data.put("two", Integer.valueOf(2));

        assertEquals("ACTIVE", engine.parse("{#if name eq foo}ACTIVE{#else}INACTIVE{/if}").render(data));
        assertEquals("INACTIVE", engine.parse("{#if name != foo}ACTIVE{#else}INACTIVE{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one < two}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one >= one}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one >= 0}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one == one}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if one is 2}NOK{#else if name eq foo}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name is foo}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if two is 2}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name != null}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name is null}NOK{#else}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if !false}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if true && true}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name is 'foo' && true}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if true && true && true}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if false || true}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if false || false || true}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if name or true}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if !(true && false)}OK{/if}").render(data));
        assertEquals("OK", engine.parse("{#if two > 1 && two < 10}OK{/if}").render(data));
    }

    @Test
    public void testNestedIf() {
        Engine engine = Engine.builder().addDefaults().build();
        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("nok", false);
        assertEquals("OK", engine.parse("{#if ok}{#if !nok}OK{/}{#else}NOK{/if}").render(data));
    }

    @Test
    public void testCompositeParameters() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("OK", engine.parse("{#if (true || false) && true}OK{/if}").render());
        assertEquals("OK", engine.parse("{#if (true || false) && true && !false}OK{/if}").render());
        assertEquals("OK", engine.parse("{#if  true && true && !(true || false)}NOK{#else}OK{/if}").render());
        assertEquals("OK", engine.parse("{#if true && true  && !(true && false)}OK{#else}NOK{/if}").render());
        assertEquals("OK", engine.parse("{#if true && !true && (true || false)}NOK{#else}OK{/if}").render());
        assertEquals("OK", engine.parse("{#if true && (true  && ( true && false))}NOK{#else}OK{/if}").render());
        assertEquals("OK", engine.parse("{#if true && (!false || false || (true || false))}OK{#else}NOK{/if}").render());
        assertEquals("OK", engine.parse("{#if (foo.or(false) || false || true) && (true)}OK{/if}").render());
        assertEquals("NOK", engine.parse("{#if foo.or(false) || false}OK{#else}NOK{/if}").render());
        assertEquals("OK", engine.parse("{#if false || (foo.or(false) || (false || true))}OK{#else}NOK{/if}").render());
        assertEquals("NOK", engine.parse("{#if (true && false)}OK{#else}NOK{/if}").render());
        assertEquals("OK", engine.parse("{#if true && true}OK{#else}NOK{/if}").render());
        assertEquals("NOK", engine.parse("{#if true && false}OK{#else}NOK{/if}").render());
        assertEquals("NOK", engine.parse("{#if false && true}OK{#else}NOK{/if}").render());
        assertEquals("OK", engine.parse("{#if true and (true or false)}OK{#else}NOK{/if}").render());
        assertEquals("NOK", engine.parse("{#if true and (true == false)}OK{#else}NOK{/if}").render());
        assertEquals("OK", engine.parse("{#if true && (false == false)}OK{#else}NOK{/if}").render());

        Map<String, String> foo = new HashMap<>();
        foo.put("bar", "something");
        assertEquals("NOK",
                engine.parse("{#if foo.bar != 'something' && foo.bar != 'other'}OK{#else}NOK{/if}").data("foo", foo).render());
        assertEquals("OK",
                engine.parse("{#if foo.bar != 'nothing' && foo.bar != 'other'}OK{#else}NOK{/if}").data("foo", foo).render());
        assertEquals("OK",
                engine.parse("{#if foo.bar == 'something' || foo.bar != 'other'}OK{#else}NOK{/if}").data("foo", foo).render());
        assertEquals("OK", engine.parse("{#if (foo.bar == 'something') || (foo.bar == 'other')}OK{#else}NOK{/if}")
                .data("foo", foo).render());

        Map<String, String> qual = new HashMap<>();
        Template template = engine.parse(
                "{#if qual.name != 'javax.inject.Named' \n"
                        + "          && qual.name != 'javax.enterprise.inject.Any'\n"
                        + "          && qual.name != 'javax.enterprise.inject.Default'}{qual.name}{/if}");
        qual.put("name", "org.acme.MyQual");
        assertEquals("org.acme.MyQual", template
                .data("qual", qual).render());
        qual.put("name", "javax.enterprise.inject.Any");
        assertEquals("", template
                .data("qual", qual).render());

    }

    @Test
    public void testParserErrors() {
        // Missing operand
        ParserTest.assertParserError("{#if foo >}{/}", IfSectionHelper.Code.BINARY_OPERATOR_MISSING_SECOND_OPERAND,
                "Parser error: binary operator [GT] set but the second operand not present for {#if} section",
                1);
    }

    @Test
    public void testParameterParsing() {
        List<Object> params = IfSectionHelper
                .parseParams(Arrays.asList("item.price", ">", "10", "&&", "item.price", "<", "20"), null);
        assertEquals(3, params.size());
        assertEquals(Arrays.asList("item.price", Operator.GT, "10"), params.get(0));
        assertEquals(Operator.AND, params.get(1));
        assertEquals(Arrays.asList("item.price", Operator.LT, "20"), params.get(2));

        params = IfSectionHelper
                .parseParams(Arrays.asList("(item.price > 10)", "&&", "item.price", "<", "20"), null);
        assertEquals(3, params.size());
        assertEquals(Arrays.asList("item.price", Operator.GT, "10"), params.get(0));
        assertEquals(Operator.AND, params.get(1));
        assertEquals(Arrays.asList("item.price", Operator.LT, "20"), params.get(2));

        params = IfSectionHelper
                .parseParams(Arrays.asList("(item.price > 10)", "&&", "(item.price < 20)"), null);
        assertEquals(3, params.size());
        assertEquals(Arrays.asList("item.price", Operator.GT, "10"), params.get(0));
        assertEquals(Operator.AND, params.get(1));
        assertEquals(Arrays.asList("item.price", Operator.LT, "20"), params.get(2));

        params = IfSectionHelper
                .parseParams(Arrays.asList("name", "is", "'foo'", "&&", "true"), null);
        assertEquals(3, params.size());
        assertEquals(Arrays.asList("name", Operator.EQ, "'foo'"), params.get(0));
        assertEquals(Operator.AND, params.get(1));
        assertEquals("true", params.get(2));
    }

    @Test
    public void testFalsy() {
        Engine engine = Engine.builder().addDefaults().build();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "foo");
        data.put("nameEmpty", "");
        data.put("boolTrue", true);
        data.put("boolFalse", false);
        data.put("intTwo", Integer.valueOf(2));
        data.put("intZero", Integer.valueOf(0));
        data.put("list", Collections.singleton("foo"));
        data.put("setEmpty", Collections.emptySet());
        data.put("mapEmpty", Collections.emptyMap());
        data.put("array", new String[] { "foo" });
        data.put("arrayEmpty", new String[] {});
        data.put("optional", Optional.of("foo"));
        data.put("optionalEmpty", Optional.empty());

        assertEquals("1", engine.parse("{#if name}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if nameEmpty}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if boolTrue}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if boolFalse}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if intTwo}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if intZero}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if list}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if setEmpty}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if mapEmpty}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if array}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if arrayEmpty}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if optional}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if optionalEmpty}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if !arrayEmpty}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if arrayEmpty || name}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if arrayEmpty && name}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if array && intTwo}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if (array && intZero) || true}1{#else}0{/if}").render(data));
        assertEquals("0", engine.parse("{#if nonExistent.or(false)}1{#else}0{/if}").render(data));
        assertEquals("1", engine.parse("{#if !nonExistent.or(false)}1{#else}0{/if}").render(data));
    }

    @Test
    public void testStandaloneLines() {
        Engine engine = Engine.builder().addDefaults().removeStandaloneLines(true).build();
        assertEquals("BAZ\n",
                engine.parse("{#if false}\n"
                        + "FOO\n"
                        + "{#else}\n"
                        + "BAZ\n"
                        + "{/if}").render());
    }

    @Test
    public void testStandaloneLinesLinebreaks() {
        Engine engine = Engine.builder().addDefaults().removeStandaloneLines(true).build();
        assertEquals("FOO\n\n\n\n",
                engine.parse("FOO\n\n\n\n").render());
        assertEquals("FOO\n\n\n\n",
                engine.parse("FOO\n\n{#if false}\nBAZ\n{/if}\n\n\n").render());
        assertEquals("FOO\n\n",
                engine.parse("FOO\n\n{#if false}\nBAZ\n{/if}\n").render());
    }

    @Test
    public void testSafeExpression() {
        Engine engine = Engine.builder().strictRendering(true).addDefaults().build();
        try {
            engine.parse("{#if val.is.not.there}NOK{#else}OK{/if}").render();
            fail();
        } catch (TemplateException expected) {
            assertEquals(
                    "Rendering error: Key \"val\" not found in the template data map with keys [] in expression {val.is.not.there}",
                    expected.getMessage());
        }
        assertEquals("OK", engine.parse("{#if val.is.not.there??}NOK{#else}OK{/if}").render());
        assertEquals("OK", engine.parse("{#if hero??}NOK{#else}OK{/if}").render());
        assertEquals("OK", engine.parse("{#if hero??}OK{#else}NOK{/if}").data("hero", true).render());
    }

    @Test
    public void testFromageCondition() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addNamespaceResolver(NamespaceResolver.builder("ContentStatus").resolve(ec -> ContentStatus.NEW).build())
                .build();
        assertEquals("OK",
                engine.parse("{#if user && target.status == ContentStatus:NEW && !target.voted(user)}NOK{#else}OK{/if}")
                        .data("user", "Stef", "target", new Target(ContentStatus.ACCEPTED)).render());
        assertEquals("OK",
                engine.parse("{#if user && target.status == ContentStatus:NEW && !target.voted(user)}OK{#else}NOK{/if}")
                        .data("user", "Stef", "target", new Target(ContentStatus.NEW)).render());
    }

    @Test
    public void testParameterOrigin() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("  {#if item.price > 1}{/if}");
        List<Expression> expressions = template.getExpressions();
        assertEquals(2, expressions.size());
        for (Expression expression : expressions) {
            if (expression.isLiteral()) {
                assertEquals(1, expression.getLiteralValue().getNow(false));
                assertEquals(1, expression.getOrigin().getLine());
                assertEquals(3, expression.getOrigin().getLineCharacterStart());
            } else {
                assertEquals("item.price", expression.toOriginalString());
                assertEquals(1, expression.getOrigin().getLine());
                assertEquals(3, expression.getOrigin().getLineCharacterStart());
            }
        }
    }

    @Test
    public void testComparisons() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("longGtInt", engine.parse("{#if val > 10}longGtInt{/if}").data("val", 11l).render());
        assertEquals("doubleGtInt", engine.parse("{#if val > 10}doubleGtInt{/if}").data("val", 20.0).render());
        assertEquals("longGtStr", engine.parse("{#if val > '10'}longGtStr{/if}").data("val", 11l).render());
        assertEquals("longLeStr", engine.parse("{#if val <= '10'}longLeStr{/if}").data("val", 1l).render());
        assertEquals("longEqInt", engine.parse("{#if val == 10}longEqInt{/if}").data("val", 10l).render());
        assertEquals("doubleEqInt", engine.parse("{#if val == 10}doubleEqInt{/if}").data("val", 10.0).render());
        assertEquals("doubleEqFloat", engine.parse("{#if val == 10.00f}doubleEqFloat{/if}").data("val", 10.0).render());
        assertEquals("longEqLong", engine.parse("{#if val eq 10l}longEqLong{/if}").data("val", Long.valueOf(10)).render());
    }

    public static class Target {

        public ContentStatus status;

        public Target(ContentStatus status) {
            this.status = status;
        }

        public boolean voted(String user) {
            return false;
        }

    }

    public enum ContentStatus {
        NEW,
        ACCEPTED
    }

}
