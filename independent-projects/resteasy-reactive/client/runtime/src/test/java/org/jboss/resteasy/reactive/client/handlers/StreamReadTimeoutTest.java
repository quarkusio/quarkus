package org.jboss.resteasy.reactive.client.handlers;

import static org.jboss.resteasy.reactive.client.handlers.ClientSendRequestHandler.DEFAULT_STREAM_READ_TIMEOUT;
import static org.jboss.resteasy.reactive.client.handlers.ClientSendRequestHandler.streamReadTimeout;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StreamReadTimeoutTest {

    @Test
    void usesDefaultWhenReadTimeoutIsNotSet() {
        assertEquals(DEFAULT_STREAM_READ_TIMEOUT, streamReadTimeout(null));
    }

    @Test
    void keepsDefaultWhenReadTimeoutIsLower() {
        assertEquals(DEFAULT_STREAM_READ_TIMEOUT, streamReadTimeout(30_000L));
    }

    @Test
    void honorsReadTimeoutWhenHigherThanDefault() {
        assertEquals(300_000L, streamReadTimeout(300_000L));
    }
}
