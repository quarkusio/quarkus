package io.quarkus.vertx.http.deployment.webjar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class WebJarResourcesFilterResultTest {

    @Test
    void hasStream_true_whenStreamProvided() {
        InputStream stream = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
        WebJarResourcesFilter.FilterResult result = new WebJarResourcesFilter.FilterResult(stream, false);

        assertTrue(result.hasStream());
    }

    @Test
    void hasStream_false_whenStreamNull() {
        WebJarResourcesFilter.FilterResult result = new WebJarResourcesFilter.FilterResult(null, false);

        assertFalse(result.hasStream());
        assertNull(result.getStream());
    }

    @Test
    void isChanged_reflectsConstructorArg() {
        WebJarResourcesFilter.FilterResult changed = new WebJarResourcesFilter.FilterResult(null, true);
        WebJarResourcesFilter.FilterResult unchanged = new WebJarResourcesFilter.FilterResult(null, false);

        assertTrue(changed.isChanged());
        assertFalse(unchanged.isChanged());
    }

    @Test
    void getStream_returnsProvidedStream() throws IOException {
        InputStream stream = new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8));
        WebJarResourcesFilter.FilterResult result = new WebJarResourcesFilter.FilterResult(stream, false);

        assertEquals("content", new String(result.getStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void close_closesStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
        WebJarResourcesFilter.FilterResult result = new WebJarResourcesFilter.FilterResult(stream, false);

        // ByteArrayInputStream.close() is a no-op, but we verify no exception
        result.close();

    }

    @Test
    void close_nullStream_noException() throws IOException {
        WebJarResourcesFilter.FilterResult result = new WebJarResourcesFilter.FilterResult(null, false);
        result.close(); // Should not throw
    }
}
