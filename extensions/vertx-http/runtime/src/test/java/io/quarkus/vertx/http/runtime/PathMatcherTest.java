package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;

public class PathMatcherTest {

    private static final Object HANDLER = new Object();

    @Test
    public void testPrefixPathWithEndingWildcard() {
        ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/one/two/*", HANDLER).build();
        assertMatched(matcher, "/one/two");
        assertMatched(matcher, "/one/two/");
        assertMatched(matcher, "/one/two/three");
        assertNotMatched(matcher, "/one/twothree");
        assertNotMatched(matcher, "/one/tw");
        assertNotMatched(matcher, "/one");
        assertNotMatched(matcher, "/");
        assertNotMatched(matcher, "");
        final Object exactPathMatcher1 = new Object();
        final Object exactPathMatcher2 = new Object();
        final Object exactPathMatcher3 = new Object();
        final Object prefixPathMatcher1 = new Object();
        final Object prefixPathMatcher2 = new Object();
        matcher = ImmutablePathMatcher.builder().addPath("/one/two/*", prefixPathMatcher1)
                .addPath("/one/two/three", exactPathMatcher1).addPath("/one/two", exactPathMatcher2)
                .addPath("/one/two/", prefixPathMatcher1).addPath("/one/two/three/", prefixPathMatcher2)
                .addPath("/one/two/three*", prefixPathMatcher2).addPath("/one/two/three/four/", prefixPathMatcher2)
                .addPath("/one/two/three/four", exactPathMatcher3).build();
        assertMatched(matcher, "/one/two/three", exactPathMatcher1);
        assertMatched(matcher, "/one/two", exactPathMatcher2);
        assertMatched(matcher, "/one/two/three/four", exactPathMatcher3);
        assertMatched(matcher, "/one/two/three/fou", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/three/four/", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/three/five", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/three/", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/thre", prefixPathMatcher1);
        assertMatched(matcher, "/one/two/", prefixPathMatcher1);
        assertNotMatched(matcher, "/one/tw");
        assertNotMatched(matcher, "/one/");
        assertNotMatched(matcher, "/");
        assertNotMatched(matcher, "");
    }

    @Test
    public void testPrefixPathDefaultHandler() {
        final Object defaultHandler = new Object();
        ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/one/two*", HANDLER)
                .addPath("/*", defaultHandler).addPath("/q*", HANDLER).build();
        assertMatched(matcher, "/", defaultHandler);
        assertMatched(matcher, "", defaultHandler);
        assertMatched(matcher, "0", defaultHandler);
        assertMatched(matcher, "/q");
        assertMatched(matcher, "/q/dev-ui");
        assertMatched(matcher, "/qE", defaultHandler);
        assertMatched(matcher, "/one/two");
        assertMatched(matcher, "/one/two/three");
        assertMatched(matcher, "/one/twothree", defaultHandler);
        final Object exactPathMatcher1 = new Object();
        final Object exactPathMatcher2 = new Object();
        final Object exactPathMatcher3 = new Object();
        final Object prefixPathMatcher1 = new Object();
        final Object prefixPathMatcher2 = new Object();
        matcher = ImmutablePathMatcher.builder().addPath("/one/two/*", prefixPathMatcher1).addPath("/*", defaultHandler)
                .addPath("/one/two/three", exactPathMatcher1).addPath("/one/two/three/", prefixPathMatcher2)
                .addPath("/one/two", exactPathMatcher2).addPath("/one/two/", prefixPathMatcher1)
                .addPath("/one/two/three*", prefixPathMatcher2).addPath("/one/two/three/four", exactPathMatcher3)
                .addPath("/one/two/three/four/", prefixPathMatcher2).build();
        assertMatched(matcher, "/one/two/three", exactPathMatcher1);
        assertMatched(matcher, "/one/two", exactPathMatcher2);
        assertMatched(matcher, "/one/two/three/four", exactPathMatcher3);
        assertMatched(matcher, "/one/two/three/fou", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/three/four/", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/three/five", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/three/", prefixPathMatcher2);
        assertMatched(matcher, "/one/two/thre", prefixPathMatcher1);
        assertMatched(matcher, "/one/two/", prefixPathMatcher1);
        assertMatched(matcher, "/one/tw", defaultHandler);
        assertMatched(matcher, "/one/", defaultHandler);
        assertMatched(matcher, "/", defaultHandler);
        assertMatched(matcher, "", defaultHandler);
    }

    @Test
    public void testPrefixPathsNoDefaultHandlerNoExactPath() {
        final Object handler1 = new Object();
        final Object handler2 = new Object();
        final ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/one/two*", handler1)
                .addPath("/q*", handler2).build();
        assertNotMatched(matcher, "/");
        assertNotMatched(matcher, "");
        assertNotMatched(matcher, "0");
        assertMatched(matcher, "/q", handler2);
        assertMatched(matcher, "/q/dev-ui", handler2);
        assertNotMatched(matcher, "/qE");
        assertMatched(matcher, "/one/two", handler1);
        assertMatched(matcher, "/one/two/three", handler1);
        assertMatched(matcher, "/one/two/", handler1);
        assertNotMatched(matcher, "/one/twothree");
    }

    @Test
    public void testSpecialChars() {
        // strictly speaking query params are not part of request path passed to the matcher
        // but here they are treated like any other character different from path separator
        final Object handler1 = new Object();
        final Object handler2 = new Object();
        final Object handler3 = new Object();
        final Object handler4 = new Object();
        final Object handler5 = new Object();
        // with default handler
        ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/one/two#three", handler2)
                .addPath("/one/two?three=four", handler1).addPath("/one/*/three?one\\\\\\=two", handler3)
                .addPath("/one/two#three*", handler4).addPath("/*/two#three*", handler5).addPath("/*", HANDLER)
                .addPath("/one/two#three/", handler4).build();
        assertMatched(matcher, "/one/two#three", handler2);
        assertMatched(matcher, "/one/two?three=four", handler1);
        assertMatched(matcher, "/one/any-value/three?one\\\\\\=two", handler3);
        assertMatched(matcher, "/one/two/three?one\\\\\\=two", handler3);
        assertMatched(matcher, "/one/two/three?one\\=two");
        assertMatched(matcher, "/one/two/three?one\\\\\\=two-three");
        assertMatched(matcher, "/one/two/three?one");
        assertMatched(matcher, "/one/two/three?");
        assertMatched(matcher, "/one/two#three?");
        assertMatched(matcher, "/one/two#thre");
        assertMatched(matcher, "/one/two");
        assertMatched(matcher, "/one/two?three=four#");
        assertMatched(matcher, "/one/two?three=fou");
        assertMatched(matcher, "/one/two#three/", handler4);
        assertMatched(matcher, "/one/two#three/christmas!", handler4);
        assertMatched(matcher, "/one/two#thre");
        assertMatched(matcher, "/one1/two#three", handler5);
        assertMatched(matcher, "/one1/two#three/", handler5);
        assertMatched(matcher, "/one1/two#three/christmas!", handler5);
        assertMatched(matcher, "/one1/two#thre");
        // no default handler
        matcher = ImmutablePathMatcher.builder().addPath("/one/two#three", handler2).addPath("/one/two#three/", handler4)
                .addPath("/one/two?three=four", handler1).addPath("/one/*/three?one\\\\\\=two", handler3)
                .addPath("/one/two#three*", handler4).addPath("/*/two#three*", handler5).build();
        assertMatched(matcher, "/one/two#three", handler2);
        assertMatched(matcher, "/one/two?three=four", handler1);
        assertMatched(matcher, "/one/any-value/three?one\\\\\\=two", handler3);
        assertMatched(matcher, "/one/two/three?one\\\\\\=two", handler3);
        assertNotMatched(matcher, "/one/two/three?one\\=two");
        assertNotMatched(matcher, "/one/two/three?one\\\\\\=two-three");
        assertNotMatched(matcher, "/one/two/three?one");
        assertNotMatched(matcher, "/one/two/three?");
        assertNotMatched(matcher, "/one/two#three?");
        assertNotMatched(matcher, "/one/two#thre");
        assertNotMatched(matcher, "/one/two");
        assertNotMatched(matcher, "/one/two?three=four#");
        assertNotMatched(matcher, "/one/two?three=fou");
        assertMatched(matcher, "/one/two#three/", handler4);
        assertMatched(matcher, "/one/two#three/christmas!", handler4);
        assertNotMatched(matcher, "/one/two#thre");
        assertMatched(matcher, "/one1/two#three", handler5);
        assertMatched(matcher, "/one1/two#three/", handler5);
        assertMatched(matcher, "/one1/two#three/christmas!", handler5);
        assertNotMatched(matcher, "/one1/two#thre");
    }

    @Test
    public void testInnerWildcardsWithExactMatches() {
        final Object handler1 = new Object();
        final Object handler2 = new Object();
        final Object handler3 = new Object();
        final Object handler4 = new Object();
        final Object handler5 = new Object();
        final Object handler6 = new Object();
        final Object handler7 = new Object();
        final Object handler8 = new Object();
        final ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/one/two", handler1)
                .addPath("/one/two/three", handler2).addPath("/one/two/three/four", handler3)
                .addPath("/", handler4).addPath("/*", HANDLER).addPath("/one/two/*/four", handler5)
                .addPath("/one/*/three/four", handler6).addPath("/*/two/three/four", handler7)
                .addPath("/*/two", handler8).addPath("/*/two/three/four/", HANDLER).build();
        assertMatched(matcher, "/one/two", handler1);
        assertMatched(matcher, "/one/two/three", handler2);
        assertMatched(matcher, "/one/two/three/four", handler3);
        assertMatched(matcher, "/", handler4);
        assertMatched(matcher, "");
        assertMatched(matcher, "no-one-likes-us");
        assertMatched(matcher, "/one/two/we-do-not-care/four", handler5);
        assertMatched(matcher, "/one/two/we-do-not-care/four/4");
        assertMatched(matcher, "/one/we-are-millwall/three/four", handler6);
        assertMatched(matcher, "/1-one/we-are-millwall/three/four");
        assertMatched(matcher, "/super-millwall/two/three/four", handler7);
        assertMatched(matcher, "/super-millwall/two/three/four/");
        assertMatched(matcher, "/super-millwall/two/three/four/1");
        assertMatched(matcher, "/from-the-den/two", handler8);
        assertMatched(matcher, "/from-the-den/two2");
    }

    @Test
    public void testInnerWildcardsOnly() {
        final Object handler1 = new Object();
        final Object handler2 = new Object();
        final Object handler3 = new Object();
        final Object handler4 = new Object();
        final Object handler5 = new Object();
        // with default path handler
        ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/*/two", handler2)
                .addPath("/*/*/three", handler1).addPath("/one/*/three", handler3)
                .addPath("/one/two/*/four", handler4).addPath("/one/two/three/*/five", handler5)
                .addPath("/*", HANDLER).build();
        assertMatched(matcher, "/any-value");
        assertMatched(matcher, "/one/two/three/four/five", handler5);
        assertMatched(matcher, "/one/two/three/4/five", handler5);
        assertMatched(matcher, "/one/two/three/sergey/five", handler5);
        assertMatched(matcher, "/one/two/three/sergey/five-ish");
        assertMatched(matcher, "/one/two/three/sergey/five/", handler5);
        assertMatched(matcher, "/one/two/three/four", handler4);
        assertMatched(matcher, "/one/two/3/four", handler4);
        assertMatched(matcher, "/one/two/three", handler3);
        assertMatched(matcher, "/one/2/three", handler3);
        assertMatched(matcher, "/one/some-very-long-text/three", handler3);
        assertMatched(matcher, "/two");
        assertMatched(matcher, "/two/two", handler2);
        assertMatched(matcher, "/2/two", handler2);
        assertMatched(matcher, "/ho-hey/two", handler2);
        assertMatched(matcher, "/ho-hey/two2");
        assertMatched(matcher, "/ho-hey/two2/");
        assertMatched(matcher, "/ho-hey/two/", handler2);
        assertMatched(matcher, "/ho-hey/hey-ho/three", handler1);
        assertMatched(matcher, "/1/2/three", handler1);
        assertMatched(matcher, "/1/two/three", handler1);
        assertMatched(matcher, "/1/two/three/", handler1);
        assertMatched(matcher, "/1/two/three/f");
        // no default path handler
        matcher = ImmutablePathMatcher.builder().addPath("/*/two", handler2)
                .addPath("/*/*/three", handler1).addPath("/one/*/three", handler3)
                .addPath("/one/two/*/four", handler4).addPath("/one/two/three/*/five", handler5).build();
        assertNotMatched(matcher, "/any-value");
        assertMatched(matcher, "/one/two/three/four/five", handler5);
        assertMatched(matcher, "/one/two/three/4/five", handler5);
        assertMatched(matcher, "/one/two/three/sergey/five", handler5);
        assertMatched(matcher, "/one/two/three/sergey/five/", handler5);
        assertNotMatched(matcher, "/one/two/three/sergey/five-ish");
        assertMatched(matcher, "/one/two/three/four", handler4);
        assertMatched(matcher, "/one/two/3/four", handler4);
        assertMatched(matcher, "/one/two/three", handler3);
        assertMatched(matcher, "/one/2/three", handler3);
        assertMatched(matcher, "/one/some-very-long-text/three", handler3);
        assertNotMatched(matcher, "/two");
        assertMatched(matcher, "/two/two", handler2);
        assertMatched(matcher, "/2/two", handler2);
        assertMatched(matcher, "/ho-hey/two", handler2);
        assertMatched(matcher, "/ho-hey/two/", handler2);
        assertNotMatched(matcher, "/ho-hey/two2");
        assertNotMatched(matcher, "/ho-hey/two2/");
        assertMatched(matcher, "/ho-hey/hey-ho/three", handler1);
        assertMatched(matcher, "/1/2/three", handler1);
        assertMatched(matcher, "/1/two/three", handler1);
        assertMatched(matcher, "/1/two/three/", handler1);
        assertNotMatched(matcher, "/1/two/three/f");
    }

    @Test
    public void testInnerWildcardWithEndingWildcard() {
        final Object handler1 = new Object();
        final Object handler2 = new Object();
        final Object handler3 = new Object();
        final Object handler4 = new Object();
        final Object handler5 = new Object();
        // with default handler
        ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/*/two/*", handler1)
                .addPath("/one/*/*", handler2).addPath("/one/two/*/four*", handler3)
                .addPath("/one/*/three/*", handler4).addPath("/one/two/*/*", handler5)
                .addPath("/*", HANDLER).build();
        assertMatched(matcher, "/one/two/three/four/five/six", handler3);
        assertMatched(matcher, "/one/two/three/four/five", handler3);
        assertMatched(matcher, "/one/two/three/four/", handler3);
        assertMatched(matcher, "/one/two/three/four", handler3);
        assertMatched(matcher, "/one/two/3/four", handler3);
        assertMatched(matcher, "/one/two/three/4", handler5);
        assertMatched(matcher, "/one/two/three/4/", handler5);
        assertMatched(matcher, "/one/two/three/4/five", handler5);
        assertMatched(matcher, "/one/2/three/four/five", handler4);
        assertMatched(matcher, "/one/2/3/four/five", handler2);
        assertMatched(matcher, "/1/two/three/four/five", handler1);
        assertMatched(matcher, "/1/2/three/four/five");
    }

    @Test
    public void testInnerWildcardsDefaultHandler() {
        final Object handler1 = new Object();
        final Object handler2 = new Object();
        final Object handler3 = new Object();
        // both default root path handler and sub-path handler
        ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/*/*", handler1)
                .addPath("/*/*/three", handler3).addPath("/*", handler2).build();
        assertMatched(matcher, "/one/two/three", handler3);
        assertMatched(matcher, "/one/two/four", handler1);
        assertMatched(matcher, "/one/two", handler1);
        assertMatched(matcher, "/one", handler2);
        assertMatched(matcher, "/", handler2);
    }

    @Test
    public void testInvalidPathPattern() {
        // path must start with a path separator
        assertThrows(IllegalArgumentException.class, () -> ImmutablePathMatcher.builder().addPath("one", HANDLER).build());
        // inner wildcard must always be only path segment character
        assertThrows(ConfigurationException.class, () -> ImmutablePathMatcher.builder().addPath("/one*/", HANDLER).build());
        assertThrows(ConfigurationException.class, () -> ImmutablePathMatcher.builder().addPath("/*one/", HANDLER).build());
        assertThrows(ConfigurationException.class, () -> ImmutablePathMatcher.builder().addPath("/o*ne/", HANDLER).build());
        assertThrows(ConfigurationException.class, () -> ImmutablePathMatcher.builder().addPath("/one/*two/", HANDLER).build());
        assertThrows(ConfigurationException.class, () -> ImmutablePathMatcher.builder().addPath("/one/*two/", HANDLER).build());
        assertThrows(ConfigurationException.class, () -> ImmutablePathMatcher.builder().addPath("/one/two*/", HANDLER).build());
        assertThrows(ConfigurationException.class,
                () -> ImmutablePathMatcher.builder().addPath("/one/*two*/", HANDLER).build());
    }

    @Test
    public void testExactPathHandlerMerging() {
        List<String> handler1 = new ArrayList<>();
        handler1.add("Neo");
        List<String> handler2 = new ArrayList<>();
        handler2.add("Trinity");
        List<String> handler3 = new ArrayList<>();
        handler3.add("Morpheus");
        var matcher = ImmutablePathMatcher.<List<String>> builder().handlerAccumulator(List::addAll)
                .addPath("/exact-path", handler1).addPath("/exact-path", handler2)
                .addPath("/exact-not-matched", handler3).build();
        var handler = matcher.match("/exact-path").getValue();
        assertNotNull(handler);
        assertTrue(handler.contains("Neo"));
        assertTrue(handler.contains("Trinity"));
        assertEquals(2, handler.size());
        handler = matcher.match("/exact-not-matched").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
    }

    @Test
    public void testDefaultHandlerMerging() {
        List<String> handler1 = new ArrayList<>();
        handler1.add("Neo");
        List<String> handler2 = new ArrayList<>();
        handler2.add("Trinity");
        List<String> handler3 = new ArrayList<>();
        handler3.add("Morpheus");
        var matcher = ImmutablePathMatcher.<List<String>> builder().handlerAccumulator(List::addAll)
                .addPath("/*", handler1).addPath("/*", handler2)
                .addPath("/", handler3).build();
        var handler = matcher.match("/default-path-handler").getValue();
        assertNotNull(handler);
        assertTrue(handler.contains("Neo"));
        assertTrue(handler.contains("Trinity"));
        assertEquals(2, handler.size());
        handler = matcher.match("/").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
    }

    @Test
    public void testPrefixPathHandlerMerging() {
        List<String> handler1 = new ArrayList<>();
        handler1.add("Neo");
        List<String> handler2 = new ArrayList<>();
        handler2.add("Trinity");
        List<String> handler3 = new ArrayList<>();
        handler3.add("Morpheus");
        List<String> handler4 = new ArrayList<>();
        handler4.add("AgentSmith");
        List<String> handler5 = new ArrayList<>();
        handler5.add("TheOracle");
        List<String> handler6 = new ArrayList<>();
        handler6.add("AgentBrown");
        var matcher = ImmutablePathMatcher.<List<String>> builder().handlerAccumulator(List::addAll).addPath("/path*", handler1)
                .addPath("/path*", handler2).addPath("/path/*", handler3).addPath("/path/", handler4)
                .addPath("/path/*/", handler5).addPath("/*", handler6).addPath("/path", handler1).build();
        var handler = matcher.match("/path").getValue();
        assertNotNull(handler);
        assertTrue(handler.contains("Neo"));
        assertTrue(handler.contains("Trinity"));
        assertTrue(handler.contains("Morpheus"));
        assertEquals(3, handler.size());
        handler = matcher.match("/path/").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
        assertTrue(handler.contains("AgentSmith"));
        handler = matcher.match("/stuart").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
        assertTrue(handler.contains("AgentBrown"));
        handler = matcher.match("/path/ozzy/").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
        assertTrue(handler.contains("TheOracle"));
    }

    @Test
    public void testInnerWildcardPathHandlerMerging() {
        List<String> handler1 = new ArrayList<>();
        handler1.add("Neo");
        List<String> handler2 = new ArrayList<>();
        handler2.add("Trinity");
        List<String> handler3 = new ArrayList<>();
        handler3.add("Morpheus");
        List<String> handler4 = new ArrayList<>();
        handler4.add("AgentSmith");
        List<String> handler5 = new ArrayList<>();
        handler5.add("TheOracle");
        List<String> handler6 = new ArrayList<>();
        handler6.add("AgentBrown");
        List<String> handler7 = new ArrayList<>();
        handler7.add("TheOperator");
        List<String> handler8 = new ArrayList<>();
        handler8.add("TheSpoonBoy");
        List<String> handler9 = new ArrayList<>();
        handler9.add("TheArchitect");
        List<String> handler10 = new ArrayList<>();
        handler10.add("KeyMan");
        List<String> handler11 = new ArrayList<>();
        handler11.add("Revolutions");
        List<String> handler12 = new ArrayList<>();
        handler12.add("Reloaded-1");
        List<String> handler13 = new ArrayList<>();
        handler13.add("Reloaded-2");
        List<String> handler14 = new ArrayList<>();
        handler14.add("Reloaded-3");
        var matcher = ImmutablePathMatcher.<List<String>> builder().handlerAccumulator(List::addAll)
                .addPath("/*/one", handler1).addPath("/*/*", handler2).addPath("/*/*", handler3)
                .addPath("/*/one", handler4).addPath("/*/two", handler5).addPath("/*", handler6)
                .addPath("/one/*/three", handler7).addPath("/one/*", handler8).addPath("/one/*/*", handler9)
                .addPath("/one/*/three", handler10).addPath("/one/*/*", handler11)
                .addPath("/one/*/*/*", handler12).addPath("/one/*/*/*", handler13)
                .addPath("/one/*/*/*", handler14).build();
        var handler = matcher.match("/one/two/three").getValue();
        assertNotNull(handler);
        assertEquals(2, handler.size());
        assertTrue(handler.contains("TheOperator"));
        assertTrue(handler.contains("KeyMan"));
        handler = matcher.match("/one/two/three/four").getValue();
        assertNotNull(handler);
        assertEquals(3, handler.size());
        assertTrue(handler.contains("Reloaded-1"));
        assertTrue(handler.contains("Reloaded-2"));
        assertTrue(handler.contains("Reloaded-3"));
        handler = matcher.match("/one/2/3").getValue();
        assertNotNull(handler);
        assertEquals(2, handler.size());
        assertTrue(handler.contains("TheArchitect"));
        assertTrue(handler.contains("Revolutions"));
        handler = matcher.match("/one/two").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
        assertTrue(handler.contains("TheSpoonBoy"));
        handler = matcher.match("/1/one").getValue();
        assertNotNull(handler);
        assertEquals(2, handler.size());
        assertTrue(handler.contains("Neo"));
        assertTrue(handler.contains("AgentSmith"));
        handler = matcher.match("/1/two").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
        assertTrue(handler.contains("TheOracle"));
        handler = matcher.match("/father-brown").getValue();
        assertNotNull(handler);
        assertEquals(1, handler.size());
        assertTrue(handler.contains("AgentBrown"));
        handler = matcher.match("/welcome/to/the/jungle").getValue();
        assertNotNull(handler);
        assertEquals(2, handler.size());
        assertTrue(handler.contains("Trinity"));
        assertTrue(handler.contains("Morpheus"));
    }

    @Test
    public void testDefaultHandlerInnerWildcardAndEndingWildcard() {
        // calling it default handler inner wildcard because first '/' path is matched and then '/one*'
        // '/one*' is matched as prefix path
        final ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/*/one*", HANDLER).build();
        assertMatched(matcher, "/1/one");
        assertMatched(matcher, "/2/one");
        assertMatched(matcher, "/3/one");
        assertMatched(matcher, "/4/one");
        assertMatched(matcher, "/4/one");
        assertMatched(matcher, "/1/one/");
        assertMatched(matcher, "/1/one/two");
        assertNotMatched(matcher, "/");
        assertNotMatched(matcher, "/1");
        assertNotMatched(matcher, "/1/");
        assertNotMatched(matcher, "/1/one1");
        assertNotMatched(matcher, "/1/two");
        assertNotMatched(matcher, "/1/on");
    }

    @Test
    public void testDefaultHandlerOneInnerWildcard() {
        // calling it default handler inner wildcard because first '/' path is matched and then '/one'
        // '/one' is matched as exact path
        final ImmutablePathMatcher<Object> matcher = ImmutablePathMatcher.builder().addPath("/*/one", HANDLER).build();
        assertMatched(matcher, "/1/one");
        assertMatched(matcher, "/2/one");
        assertMatched(matcher, "/3/one");
        assertMatched(matcher, "/4/one");
        assertMatched(matcher, "/4/one");
        assertMatched(matcher, "/1/one/");
        assertNotMatched(matcher, "/");
        assertNotMatched(matcher, "/1");
        assertNotMatched(matcher, "/1/");
        assertNotMatched(matcher, "/1/two");
        assertNotMatched(matcher, "/1/one1");
        assertNotMatched(matcher, "/1/on");
        assertNotMatched(matcher, "/1/one/two");
    }

    private static void assertMatched(ImmutablePathMatcher<Object> matcher, String path, Object handler) {
        var match = matcher.match(path);
        assertEquals(handler, match.getValue());
    }

    private static void assertMatched(ImmutablePathMatcher<Object> matcher, String path) {
        assertMatched(matcher, path, HANDLER);
    }

    private static <T> void assertNotMatched(ImmutablePathMatcher<T> matcher, String path) {
        var match = matcher.match(path);
        assertNull(match.getValue());
    }

}
