package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    public void testMultipleDefaultValues() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build();
        Template template = engine
                .parse("{@java.lang.String val='foo'}\n{@int anotherVal=1}\n{val}::{anotherVal}");
        assertEquals("foo::2", template.data("anotherVal", 2).render());
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
