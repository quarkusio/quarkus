package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

public class ExpressionTest {

    @Test
    public void testNamespace() throws InterruptedException, ExecutionException {
        verify("data:name.value", "data", ImmutableList.of("name", "value"), null);
        verify("name.value", null, ImmutableList.of("name", "value"), null);
        verify("name[value]", null, ImmutableList.of("name", "value"), null);
        verify("0", null, ImmutableList.of("0"), CompletableFuture.completedFuture(Integer.valueOf(0)));
        verify("false", null, ImmutableList.of("false"), CompletableFuture.completedFuture(Boolean.FALSE));
        verify("null", null, ImmutableList.of("null"), CompletableFuture.completedFuture(null));
        verify("name.orElse('John')", null, ImmutableList.of("name", "orElse('John')"), null);
        verify("name or 'John'", null, ImmutableList.of("name", "or('John')"), null);
        verify("item.name or 'John'", null, ImmutableList.of("item", "name", "or('John')"), null);
        verify("name.func('John', 1)", null, ImmutableList.of("name", "func('John', 1)"), null);
        verify("name ?: 'John Bug'", null, ImmutableList.of("name", "?:('John Bug')"), null);
        verify("name ? 'John' : 'Bug'", null, ImmutableList.of("name", "?('John')", ":('Bug')"), null);
        verify("name.func(data:foo)", null, ImmutableList.of("name", "func(data:foo)"), null);
    }

    private void verify(String value, String namespace, List<String> parts, CompletableFuture<Object> literal)
            throws InterruptedException, ExecutionException {
        Expression exp = Expression.from(value);
        assertEquals(namespace, exp.namespace);
        assertEquals(parts, exp.parts);
        if (literal == null) {
            assertNull(exp.literal);
        } else {
            assertNotNull(exp.literal);
            assertEquals(literal.get(), exp.literal.get());
        }
    }

}
