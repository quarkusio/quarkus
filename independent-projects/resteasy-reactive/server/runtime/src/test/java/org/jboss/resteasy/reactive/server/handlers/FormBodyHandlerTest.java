package org.jboss.resteasy.reactive.server.handlers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class FormBodyHandlerTest {
    @Test
    public void capturingInputStreamDiscardsNegativeOne() throws IOException {
        final String string = "this is a string";
        final ByteArrayInputStream bais = new ByteArrayInputStream(string.getBytes(UTF_8));
        final FormBodyHandler.CapturingInputStream captured = new FormBodyHandler.CapturingInputStream(bais);
        assertEquals(string, new String(captured.readAllBytes(), UTF_8));
        assertEquals(string, captured.baos.toString(UTF_8));
    }
}
