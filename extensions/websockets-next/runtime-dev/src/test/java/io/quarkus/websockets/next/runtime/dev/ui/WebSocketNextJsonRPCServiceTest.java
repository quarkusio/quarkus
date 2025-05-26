package io.quarkus.websockets.next.runtime.dev.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WebSocketNextJsonRPCServiceTest {

    @Test
    public void testIsInvalidPath() {
        assertFalse(WebSocketNextJsonRPCService.isInvalidPath("/echo", "/echo"));
        assertFalse(WebSocketNextJsonRPCService.isInvalidPath("/echo?foo=1", "/echo"));
        assertFalse(WebSocketNextJsonRPCService.isInvalidPath("/echo/alpha", "/echo/alpha"));
        assertTrue(WebSocketNextJsonRPCService.isInvalidPath("/echo", "/echo/alpha"));
        assertTrue(WebSocketNextJsonRPCService.isInvalidPath("/echo", "/echo/{alpha}"));
        assertTrue(WebSocketNextJsonRPCService.isInvalidPath("/echo/1/baz", "/echo/{alpha}_1/baz"));
        assertFalse(WebSocketNextJsonRPCService.isInvalidPath("/echo/joe_1/baz", "/echo/{alpha}_1/baz"));
        assertFalse(WebSocketNextJsonRPCService.isInvalidPath("/echo/joe_1foo/baz", "/echo/{alpha}_1{bravo}/baz"));
        assertTrue(WebSocketNextJsonRPCService.isInvalidPath("/echos/1/baz", "/echo/{alpha}/baz"));
    }
}
