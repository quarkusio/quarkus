package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.CacheSectionHelper.Cache;

public class CacheSectionTest {

    @Test
    public void testCached() {
        ConcurrentMap<String, CompletionStage<ResultNode>> map = new ConcurrentHashMap<>();
        Engine engine = engineWithCache(map);

        Template template = engine.parse("{#cached}{counter.val}{/cached}", null, "foo.html");
        Counter counter = new Counter();

        assertEquals("1", template.data("counter", counter).render());
        assertEquals(1, map.size());
        // {counter.val} was cached
        assertEquals("1", template.data("counter", counter).render());
        // Invalidate all cache entries
        map.clear();
        assertEquals("2", template.data("counter", counter).render());
        assertEquals("2", template.data("counter", counter).render());
        assertEquals(1, map.size());
        assertEquals("foo.html:1:1_", map.keySet().iterator().next());
    }

    @Test
    public void testCachedWithKey() {
        ConcurrentMap<String, CompletionStage<ResultNode>> map = new ConcurrentHashMap<>();
        Engine engine = engineWithCache(map);

        Template template = engine.parse("{#cached key=myKey}{counter.val}{/cached}");
        Counter counter = new Counter();

        assertEquals("1", template.data("counter", counter, "myKey", "foo").render());
        assertEquals("2", template.data("counter", counter, "myKey", "bar").render());
        assertEquals("1", template.data("counter", counter, "myKey", "foo").render());
        assertEquals("2", template.data("counter", counter, "myKey", "bar").render());
        assertEquals("3", template.data("counter", counter, "myKey", "baz").render());
        assertEquals(3, map.size());
    }

    @Test
    public void testCachedWithFailure() {
        ConcurrentMap<String, CompletionStage<ResultNode>> map = new ConcurrentHashMap<>();
        Engine engine = engineWithCache(map);

        Template template = engine.parse("{#cached}{counter.getVal(fail)}{/cached}");
        Counter counter = new Counter();

        assertThrows(IllegalStateException.class, () -> template.data("counter", counter, "fail", true).render());
        // The failure is cached
        assertThrows(IllegalStateException.class, () -> template.data("counter", counter, "fail", false).render());
        assertEquals(1, map.size());
        // Invalidate all cache entries
        map.clear();
        assertEquals("1", template.data("counter", counter, "fail", false).render());
        // The success is cached
        assertEquals("1", template.data("counter", counter, "fail", true).render());
        assertEquals(1, map.size());
    }

    private Engine engineWithCache(ConcurrentMap<String, CompletionStage<ResultNode>> map) {
        return Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new CacheSectionHelper.Factory(new Cache() {
                    @Override
                    public CompletionStage<ResultNode> getValue(String key,
                            Function<String, CompletionStage<ResultNode>> loader) {
                        return map.computeIfAbsent(key, k -> loader.apply(k));
                    }
                })).build();
    }

    public static class Counter {

        private final AtomicInteger val = new AtomicInteger();

        public int getVal() {
            return val.incrementAndGet();
        }

        public int getVal(boolean failure) {
            if (failure) {
                throw new IllegalStateException();
            }
            return val.incrementAndGet();
        }

    }

}
