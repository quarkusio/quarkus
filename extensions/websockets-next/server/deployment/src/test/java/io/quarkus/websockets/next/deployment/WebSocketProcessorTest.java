package io.quarkus.websockets.next.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.quarkus.websockets.next.WebSocketServerException;

public class WebSocketProcessorTest {

    @Test
    public void testGetPath() {
        assertEquals("/foo/:id", WebSocketProcessor.getPath("/foo/{id}"));
        assertEquals("/foo/:id/bar/:id2", WebSocketProcessor.getPath("/foo/{id}/bar/{id2}"));
        assertEquals("/foo/:bar-:baz", WebSocketProcessor.getPath("/foo/{bar}-{baz}"));
        assertEquals("/ws/v:version", WebSocketProcessor.getPath("/ws/v{version}"));
        WebSocketServerException e = assertThrows(WebSocketServerException.class,
                () -> WebSocketProcessor.getPath("/foo/v{bar}/{baz}and{alpha_1}-{name}"));
        assertEquals(
                "Path parameter {baz} may not be followed by an alphanumeric character or underscore: /foo/v{bar}/{baz}and{alpha_1}-{name}",
                e.getMessage());
        e = assertThrows(WebSocketServerException.class,
                () -> WebSocketProcessor.getPath("/foo/v{bar}/{baz}_{alpha_1}-{name}"));
        assertEquals(
                "Path parameter {baz} may not be followed by an alphanumeric character or underscore: /foo/v{bar}/{baz}_{alpha_1}-{name}",
                e.getMessage());
        e = assertThrows(WebSocketServerException.class,
                () -> WebSocketProcessor.getPath("/foo/v{bar}/{baz}1-{name}"));
        assertEquals(
                "Path parameter {baz} may not be followed by an alphanumeric character or underscore: /foo/v{bar}/{baz}1-{name}",
                e.getMessage());
    }

    @Test
    public void testMergePath() {
        assertEquals("foo/bar", WebSocketProcessor.mergePath("foo/", "/bar"));
        assertEquals("foo/bar", WebSocketProcessor.mergePath("foo", "/bar"));
        assertEquals("foo/bar", WebSocketProcessor.mergePath("foo/", "bar"));
    }

}
