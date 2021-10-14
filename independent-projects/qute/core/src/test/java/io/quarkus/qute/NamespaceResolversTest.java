package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class NamespaceResolversTest {

    @Test
    public void testMultipleSamePriority() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Engine.builder()
                        .addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> null).build())
                        .addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> null).build()))
                .withMessageStartingWith("Namespace [foo] may not be handled by multiple resolvers of the same priority [1]:");
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
        // This one should we used first but returns NOT_FOUND and so the other resolver is used
        Engine engine = Engine.builder().addValueResolver(new ReflectionValueResolver())
                .addNamespaceResolver(NamespaceResolver.builder("foo").resolve(e -> "foo1").build())
                .addNamespaceResolver(NamespaceResolver.builder("foo").priority(50).resolve(Results.NotFound::from).build())
                .build();
        assertEquals("FOO1", engine.parse("{foo:baz.toUpperCase}").render());
    }

    @Test
    public void testInvalidNamespace() {
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> Engine.builder()
                        .addNamespaceResolver(NamespaceResolver.builder("foo:").resolve(ec -> "foo").build()))
                .withMessageContaining("[foo:] is not a valid namespace");

        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> Engine.builder().addNamespaceResolver(new NamespaceResolver() {
                    @Override
                    public CompletionStage<Object> resolve(EvalContext context) {
                        return null;
                    }

                    @Override
                    public String getNamespace() {
                        return "$#%$%#$%";
                    }
                }))
                .withMessageContaining("[$#%$%#$%] is not a valid namespace");
    }

    @Test
    public void testNoNamespaceFound() {
        assertThatExceptionOfType(TemplateException.class)
                .isThrownBy(() -> Engine.builder().addDefaults().build().parse("{charlie:name}", null, "alpha.html").render())
                .withMessage(
                        "No namespace resolver found for [charlie] in expression {charlie:name} in template alpha.html on line 1");
    }

}
