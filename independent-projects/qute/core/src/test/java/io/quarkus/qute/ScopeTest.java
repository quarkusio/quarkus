package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ScopeTest {

    @Test
    public void testSanitizeType() {
        assertBinding("List<? extends Foo>", "List<Foo>");
        assertBinding("List<? super Foo>", "List<Foo>");
        assertBinding("List< ? super  Foo>", "List<Foo>");
        assertBinding("List<?>", "List<java.lang.Object>");
        assertBinding("Map<String, ?>", "Map<String,java.lang.Object>");
        assertBinding("Map<String, ? extends Foo>", "Map<String,Foo>");
        assertBinding("Map<String, ? extends List<Foo>>", "Map<String,List<Foo>>");
        assertBinding("Map<? extend java.lang.Object, ?>", "Map<java.lang.Object,java.lang.Object>");
    }

    private void assertBinding(String type, String sanitizedType) {
        Scope scope = new Scope(null);
        scope.putBinding("foo", sanitizedType);
        assertEquals(sanitizedType, scope.getBinding("foo"));
    }

}
