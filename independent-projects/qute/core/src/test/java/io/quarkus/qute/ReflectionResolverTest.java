package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class ReflectionResolverTest {

    @Test
    public void testReflectionResolver() {
        Map<Integer, String> treeMap = new TreeMap<>(Integer::compare);
        treeMap.put(2, "bar");
        treeMap.put(1, "foo");
        assertEquals("foo", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{map.entrySet.iterator.next.value}").data("map", treeMap).render());
    }

}
