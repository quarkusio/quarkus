package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.qute.Expression.Part;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

public class ExpressionTest {

    @Test
    public void testExpressions() throws InterruptedException, ExecutionException {
        verify("data:name.value", "data", null, name("name", "data:name"), name("value", "value"));
        verify("data:getName('value')", "data", null, virtualMethod("getName", ExpressionImpl.from("'value'")));
        // ignore adjacent separators
        verify("name..value", null, null, name("name"), name("value"));
        verify("0", null, CompletedStage.of(Integer.valueOf(0)), name("0", "|java.lang.Integer|"));
        verify("false", null, CompletedStage.of(Boolean.FALSE), name("false", "|java.lang.Boolean|"));
        verify("null", null, CompletedStage.of(null), name("null"));
        verify("name.orElse('John')", null, null, name("name"), virtualMethod("orElse", ExpressionImpl.from("'John'")));
        verify("name or 'John'", null, null, name("name"), virtualMethod("or", ExpressionImpl.from("'John'")));
        verify("item.name or 'John'", null, null, name("item"), name("name"),
                virtualMethod("or", ExpressionImpl.from("'John'")));
        verify("name.func('John', 1)", null, null, name("name"),
                virtualMethod("func", ExpressionImpl.literalFrom(-1, "'John'"), ExpressionImpl.literalFrom(-1, "1")));
        verify("name ?: 'John Bug'", null, null, name("name"),
                virtualMethod("?:", ExpressionImpl.literalFrom(-1, "'John Bug'")));
        verify("name ? 'John' : 'Bug'", null, null, name("name"), virtualMethod("?", ExpressionImpl.literalFrom(-1, "'John'")),
                virtualMethod(":", ExpressionImpl.literalFrom(-1, "'Bug'")));
        verify("name.func(data:foo)", null, null, name("name"), virtualMethod("func", ExpressionImpl.from("data:foo")));
        verify("this.getList(5).size", null, null, name("this"), virtualMethod("getList", ExpressionImpl.literalFrom(-1, "5")),
                name("size"));
        verify("foo.call(bar.baz)", null, null, name("foo"), virtualMethod("call", ExpressionImpl.from("bar.baz")));
        verify("foo.call(bar.call(1))", null, null, name("foo"), virtualMethod("call", ExpressionImpl.from("bar.call(1)")));
        verify("foo.call(bar.alpha(1),bar.alpha('ping'))", null, null, name("foo"),
                virtualMethod("call", ExpressionImpl.from("bar.alpha(1)"), ExpressionImpl.from("bar.alpha('ping')")));
        verify("'foo:bar'", null, CompletedStage.of("foo:bar"), name("'foo:bar'", "|java.lang.String|"));
        // bracket notation
        // ignore adjacent separators
        verify("name[['value']", null, null, name("name"), name("value"));
        verify("name[false]]", null, null, name("name"), name("false"));
        verify("name[1l]", null, null, name("name"), name("1"));
        try {
            verify("name['value'][1][null]", null, null);
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().contains("Null value"));
        }
        try {
            verify("name[value]", null, null);
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().contains("Non-literal value"));
        }
        //verify("name[1l]['foo']", null, null, name("name"), name("1"), name("foo"));
        verify("foo[\"name.dot\"].value", null, null, name("foo"), name("name.dot"), name("value"));
    }

    @Test
    public void testNestedVirtualMethods() {
        Expression exp = ExpressionImpl.from("movie.findServices(movie.name,movie.toNumber(movie.getName))");
        assertNull(exp.getNamespace());
        List<Expression.Part> parts = exp.getParts();
        assertEquals(2, parts.size());
        assertEquals("movie", parts.get(0).getName());
        Expression.VirtualMethodPart findServicesPart = parts.get(1).asVirtualMethod();
        List<Expression> findServicesParams = findServicesPart.getParameters();
        assertEquals(2, findServicesParams.size());
        Expression findServicesParam2 = findServicesParams.get(1);
        parts = findServicesParam2.getParts();
        assertEquals(2, parts.size());
        Expression.VirtualMethodPart toNumberPart = parts.get(1).asVirtualMethod();
        List<Expression> toNumberParams = toNumberPart.getParameters();
        assertEquals(1, toNumberParams.size());
        Expression movieGetName = toNumberParams.get(0);
        parts = movieGetName.getParts();
        assertEquals(2, parts.size());
        assertEquals("movie", parts.get(0).getName());
        assertEquals("getName", parts.get(1).getName());
    }

    @Test
    public void testTypeInfo() {
        List<String> parts = Expressions.splitTypeInfoParts("|org.acme.Foo|.call(|java.util.List<org.acme.Label>|,bar)");
        assertEquals(2, parts.size());
        assertEquals("|org.acme.Foo|", parts.get(0));
        assertEquals("call(|java.util.List<org.acme.Label>|,bar)", parts.get(1));
    }

    private void verify(String value, String namespace, CompletedStage<Object> literalValue, Part... parts)
            throws InterruptedException, ExecutionException {
        ExpressionImpl exp = ExpressionImpl.from(value);
        assertEquals(namespace, exp.getNamespace());
        assertEquals(Arrays.asList(parts), exp.getParts());
        if (literalValue == null) {
            assertNull(exp.getLiteral());
        } else {
            assertNotNull(exp.getLiteralValue());
            assertEquals(literalValue.get(), exp.getLiteral());
        }
    }

    private Part name(String name) {
        return name(name, null);
    }

    private Part name(String name, String typeInfo) {
        return new ExpressionImpl.PartImpl(name, typeInfo);
    }

    private Part virtualMethod(String name, Expression... params) {
        return new ExpressionImpl.VirtualMethodPartImpl(name, Arrays.asList(params), null);
    }

}
