package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class ReflectionResolverTest {

    @Test
    public void testReflectionResolver() {
        Map<Integer, String> treeMap = new TreeMap<>(Integer::compare);
        treeMap.put(2, "bar");
        treeMap.put(1, "foo");
        assertEquals("foo::o", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{map.entrySet.iterator.next.value}::{str.charAt(1)}").data("map", treeMap, "str", "foo").render());
    }

    @Test
    public void testFieldAccessor() {
        assertEquals("box", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.name}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testMethodWithParameter() {
        assertEquals("3", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.computeLength(foo.name)}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testMethodWithParameterNotFound() {
        assertEquals("NOT_FOUND", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.computeLength(true) ?: 'NOT_FOUND'}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testMethodWithVarargs() {
        assertEquals("box:box:", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.compute(foo.name,1,2)}").data("foo", new Foo("box")).render());
    }

    public static class Foo {

        public final String name;

        public Foo(String name) {
            this.name = name;
        }

        public int computeLength(String val) {
            return val.length();
        }

        public int computeLength(Double val) {
            return val.intValue();
        }

        public String compute(String val, int... counts) {
            StringBuilder builder = new StringBuilder();
            IntStream.of(counts).forEach(i -> builder.append(val).append(":"));
            return builder.toString();
        }

    }

}
