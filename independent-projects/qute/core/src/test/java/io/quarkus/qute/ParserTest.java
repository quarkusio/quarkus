package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class ParserTest {

    @Test
    public void testSectionEndValidation() {
        Engine engine = Engine.builder().addDefaultSectionHelpers()
                .build();
        try {
            engine.parse("{#if test}Hello {name}!{/for}");
            fail();
        } catch (IllegalStateException expected) {
            String message = expected.getMessage();
            assertTrue(message.contains("if"));
            assertTrue(message.contains("for"));
        }
    }

    @Test
    public void testUnterminatedTag() {
        Engine engine = Engine.builder().addDefaultSectionHelpers()
                .build();
        try {
            engine.parse("{#if test}Hello {name}");
            fail();
        } catch (IllegalStateException expected) {
            String message = expected.getMessage();
            assertTrue(message.contains("if"));
        }
    }

    @Test
    public void testTypeCheckInfos() {
        Engine engine = Engine.builder().addDefaultSectionHelpers()
                .build();
        Template template = engine.parse("{@org.acme.Foo foo}"
                + "{@java.util.List<org.acme.Label> labels}"
                + "{foo.name}"
                + "{#for item in foo.items}"
                + "{item.name}{bar}"
                + "{/}"
                + "{#each labels}"
                + "{it.name}"
                + "{/}"
                + "{inject:bean.name}"
                + "{#each inject:bean.labels}"
                + "{it.value}"
                + "{/}"
                + "{#set baz=foo.bar}"
                + "{baz.name}"
                + "{/}"
                + "{#with foo.bravo as delta}"
                + "{delta.id}"
                + "{/}"
                + "{#for foo in foos}"
                + "{foo.baz}"
                + "{/}");
        Set<Expression> expressions = template.getExpressions();

        assertExpr(expressions, "foo.name", 2, "[org.acme.Foo].name");
        assertExpr(expressions, "foo.items", 2, "[org.acme.Foo].items");
        assertExpr(expressions, "item.name", 2, "[org.acme.Foo].items<for-element>.name");
        assertExpr(expressions, "bar", 1, null);
        assertExpr(expressions, "labels", 1, "[java.util.List<org.acme.Label>]");
        assertExpr(expressions, "it.name", 2, "[java.util.List<org.acme.Label>]<for-element>.name");
        assertExpr(expressions, "inject:bean.name", 2, "[" + Expressions.TYPECHECK_NAMESPACE_PLACEHOLDER + "].bean.name");
        assertExpr(expressions, "inject:bean.labels", 2, "[" + Expressions.TYPECHECK_NAMESPACE_PLACEHOLDER + "].bean.labels");
        assertExpr(expressions, "it.value", 2,
                "[" + Expressions.TYPECHECK_NAMESPACE_PLACEHOLDER + "].bean.labels<for-element>.value");
        assertExpr(expressions, "foo.bar", 2, "[org.acme.Foo].bar");
        assertExpr(expressions, "baz.name", 2, "[org.acme.Foo].bar.name");
        assertExpr(expressions, "foo.bravo", 2, "[org.acme.Foo].bravo");
        assertExpr(expressions, "delta.id", 2, "[org.acme.Foo].bravo.id");
        assertExpr(expressions, "foo.baz", 2, null);
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
        assertEquals(6, find(template.getExpressions(), "foo.items").origin.getLine());
        assertEquals(8, find(template.getExpressions(), "item.name").origin.getLine());
    }

    private void assertExpr(Set<Expression> expressions, String value, int parts, String typeCheckInfo) {
        Expression expr = find(expressions, value);
        assertEquals(parts, expr.parts.size());
        assertEquals(typeCheckInfo,
                expr.typeCheckInfo);
    }

    private Expression find(Set<Expression> expressions, String val) {
        return expressions.stream().filter(e -> e.toOriginalString().equals(val)).findAny().get();
    }

}
