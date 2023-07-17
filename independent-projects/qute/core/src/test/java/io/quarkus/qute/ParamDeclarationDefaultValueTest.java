package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ParamDeclarationDefaultValueTest {

    @Test
    public void testDefaultValue() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        assertDefaultValue(engine.parse("{@java.lang.String val='foo'}\n{val.toUpperCase}"), "FOO");
        assertDefaultValue(engine.parse("{@java.lang.String val ='foo'}\n{val.toUpperCase}"), "FOO");
        assertDefaultValue(engine.parse("{@java.lang.String val = 'foo'}\n{val.toUpperCase}"), "FOO");
        assertDefaultValue(engine.parse("{@java.lang.String val= 'foo'}\n{val.toUpperCase}"), "FOO");
        assertDefaultValue(engine.parse("{@java.lang.String val='foo and bar'}\n{val.toUpperCase}"), "FOO AND BAR");
        assertDefaultValue(engine.parse("{@java.lang.String val= 'foo and bar'}\n{val.toUpperCase}"), "FOO AND BAR");
    }

    @Test
    public void testDefaultValueWithComposite() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        Template template = engine.parse("{@java.lang.String val=(foo or bar)}{val}");
        assertEquals("barbar", template.data("bar", "barbar").render());
        Expression fooExpr = template.getExpressions().stream().filter(e -> !e.isLiteral()).findFirst().orElse(null);
        assertNotNull(fooExpr);
        assertNull(fooExpr.collectTypeInfo());
    }

    @Test
    public void testMultipleDefaultValues() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        Template template = engine
                .parse("{@java.lang.String val='foo'}\n{@int anotherVal=1}\n{val}::{anotherVal}");
        assertEquals("foo::2", template.data("anotherVal", 2).render());
        List<ParameterDeclaration> parameterDeclarations = template.getParameterDeclarations();
        assertEquals(2, parameterDeclarations.size());
        for (ParameterDeclaration pd : parameterDeclarations) {
            if (pd.getKey().equals("val")) {
                assertEquals("'foo'", pd.getDefaultValue().toOriginalString());
                assertEquals("|java.lang.String|", pd.getTypeInfo());
                assertEquals(1, pd.getOrigin().getLine());
            } else {
                assertEquals("anotherVal", pd.getKey());
                assertEquals("1", pd.getDefaultValue().toOriginalString());
                assertEquals("|int|", pd.getTypeInfo());
            }
        }
    }

    private void assertDefaultValue(Template template, String expectedOutput) {
        assertEquals(expectedOutput, template.render());
        Expression fooExpr = template.getExpressions().stream().filter(e -> e.isLiteral()).findFirst().orElse(null);
        assertNotNull(fooExpr);
        assertEquals("|java.lang.String|", fooExpr.collectTypeInfo());
        Expression valExpr = template.getExpressions().stream().filter(e -> e.toOriginalString().equals("val.toUpperCase"))
                .findAny().orElse(null);
        assertNotNull(valExpr);
        assertEquals("|java.lang.String|.toUpperCase", valExpr.collectTypeInfo());
        assertEquals(2, valExpr.getOrigin().getLine());
    }

}
