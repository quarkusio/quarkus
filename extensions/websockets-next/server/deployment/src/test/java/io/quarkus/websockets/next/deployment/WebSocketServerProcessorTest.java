package io.quarkus.websockets.next.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class WebSocketServerProcessorTest {

    @Test
    public void testGetPath() {
        assertEquals("/foo/:id", WebSocketServerProcessor.getPath("/foo/{id}"));
        assertEquals("/foo/:bar-:baz", WebSocketServerProcessor.getPath("/foo/{bar}-{baz}"));
        assertEquals("/ws/v:version", WebSocketServerProcessor.getPath("/ws/v{version}"));
        assertEquals("/foo/v:bar/:bazand:alpha_1-:name",
                WebSocketServerProcessor.getPath("/foo/v{bar}/{baz}and{alpha_1}-{name}"));
    }

    @Test
    public void testMergePath() {
        assertEquals("foo/bar", WebSocketServerProcessor.mergePath("foo/", "/bar"));
        assertEquals("foo/bar", WebSocketServerProcessor.mergePath("foo", "/bar"));
        assertEquals("foo/bar", WebSocketServerProcessor.mergePath("foo/", "bar"));
    }

}
