package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class NamespaceResolversTest {

    @Test
    public void testMultipleSamePriority() {
        try {
            Engine.builder().addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> {
                return null;
            }).build()).addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> {
                return null;
            }).build());
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage()
                    .startsWith("Namespace [foo] may not be handled by multiple resolvers of the same priority [1]:"));
        }
    }

    @Test
    public void testMultipleDifferentPriority() {
        Engine engine = Engine.builder().addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> {
            return "foo1";
        }).build()).addNamespaceResolver(NamespaceResolver.builder("foo").priority(50).resolve(e -> {
            return "foo2";
        }).build()).build();
        assertEquals("foo2", engine.parse("{foo:baz}").render());
    }

    @Test
    public void testMultipleAndNotFound() {
        Engine engine = Engine.builder().addValueResolver(new ReflectionValueResolver())
                .addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> {
                    return "foo1";
                }).build()).addNamespaceResolver(NamespaceResolver.builder("foo").priority(50).resolve(e -> {
                    // This one should we used first but returns NOT_FOUND and so the other resolver is used
                    return Results.NotFound.from(e);
                }).build()).build();
        assertEquals("FOO1", engine.parse("{foo:baz.toUpperCase}").render());
    }

    @Test
    public void testInvalidNamespace() {
        try {
            Engine.builder().addNamespaceResolver(NamespaceResolver.builder("foo:").resolve(ec -> "foo").build());
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage()
                    .contains("[foo:] is not a valid namespace"));
        }
        try {
            Engine.builder().addNamespaceResolver(new NamespaceResolver() {

                @Override
                public CompletionStage<Object> resolve(EvalContext context) {
                    return null;
                }

                @Override
                public String getNamespace() {
                    return "$#%$%#$%";
                }

            });
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage()
                    .contains("[$#%$%#$%] is not a valid namespace"));
        }
    }

    @Test
    public void testNoNamespaceFound() {
        try {
            Engine.builder().addDefaults().build().parse("{charlie:name}", null, "alpha.html").render();
            fail();
        } catch (TemplateException expected) {
            assertEquals(
                    "No namespace resolver found for [charlie] in expression {charlie:name} in template alpha.html on line 1",
                    expected.getMessage());
        }
    }

}
