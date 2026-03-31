package io.quarkus.vertx.http.deployment.webjar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class CombinedWebJarResourcesFilterTest {

    @Test
    void singleFilter_passThrough() throws IOException {
        WebJarResourcesFilter passThrough = (fileName, stream) -> new WebJarResourcesFilter.FilterResult(stream, false);

        CombinedWebJarResourcesFilter combined = new CombinedWebJarResourcesFilter(List.of(passThrough));
        InputStream input = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));

        WebJarResourcesFilter.FilterResult result = combined.apply("test.txt", input);

        assertFalse(result.isChanged());
        assertTrue(result.hasStream());
        assertEquals("hello", new String(result.getStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void singleFilter_modifiesContent() throws IOException {
        WebJarResourcesFilter modifier = (fileName, stream) -> {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String modified = content.toUpperCase();
            return new WebJarResourcesFilter.FilterResult(
                    new ByteArrayInputStream(modified.getBytes(StandardCharsets.UTF_8)), true);
        };

        CombinedWebJarResourcesFilter combined = new CombinedWebJarResourcesFilter(List.of(modifier));
        InputStream input = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));

        WebJarResourcesFilter.FilterResult result = combined.apply("test.txt", input);

        assertTrue(result.isChanged());
        assertEquals("HELLO", new String(result.getStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void multipleFilters_chainedCorrectly() throws IOException {
        WebJarResourcesFilter addPrefix = (fileName, stream) -> {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new WebJarResourcesFilter.FilterResult(
                    new ByteArrayInputStream(("prefix-" + content).getBytes(StandardCharsets.UTF_8)), true);
        };

        WebJarResourcesFilter addSuffix = (fileName, stream) -> {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new WebJarResourcesFilter.FilterResult(
                    new ByteArrayInputStream((content + "-suffix").getBytes(StandardCharsets.UTF_8)), true);
        };

        CombinedWebJarResourcesFilter combined = new CombinedWebJarResourcesFilter(List.of(addPrefix, addSuffix));
        InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

        WebJarResourcesFilter.FilterResult result = combined.apply("test.txt", input);

        assertTrue(result.isChanged());
        assertEquals("prefix-data-suffix", new String(result.getStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void changedFlag_trueIfAnyFilterChanges() throws IOException {
        WebJarResourcesFilter noChange = (fileName, stream) -> new WebJarResourcesFilter.FilterResult(stream, false);
        WebJarResourcesFilter change = (fileName, stream) -> new WebJarResourcesFilter.FilterResult(stream, true);

        CombinedWebJarResourcesFilter combined = new CombinedWebJarResourcesFilter(List.of(noChange, change));
        InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

        WebJarResourcesFilter.FilterResult result = combined.apply("test.txt", input);

        assertTrue(result.isChanged());
    }

    @Test
    void changedFlag_falseIfNoFilterChanges() throws IOException {
        WebJarResourcesFilter noChange1 = (fileName, stream) -> new WebJarResourcesFilter.FilterResult(stream, false);
        WebJarResourcesFilter noChange2 = (fileName, stream) -> new WebJarResourcesFilter.FilterResult(stream, false);

        CombinedWebJarResourcesFilter combined = new CombinedWebJarResourcesFilter(List.of(noChange1, noChange2));
        InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

        WebJarResourcesFilter.FilterResult result = combined.apply("test.txt", input);

        assertFalse(result.isChanged());
    }

    @Test
    void nullStreamFromFilter_propagatedToNext() throws IOException {
        WebJarResourcesFilter nullifier = (fileName, stream) -> new WebJarResourcesFilter.FilterResult(null, true);
        WebJarResourcesFilter passThrough = (fileName, stream) -> new WebJarResourcesFilter.FilterResult(stream, false);

        CombinedWebJarResourcesFilter combined = new CombinedWebJarResourcesFilter(List.of(nullifier, passThrough));
        InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

        WebJarResourcesFilter.FilterResult result = combined.apply("test.txt", input);

        assertTrue(result.isChanged());
        assertNull(result.getStream());
        assertFalse(result.hasStream());
    }

    @Test
    void fileNamePassedToAllFilters() throws IOException {
        String[] capturedNames = new String[2];

        WebJarResourcesFilter filter1 = (fileName, stream) -> {
            capturedNames[0] = fileName;
            return new WebJarResourcesFilter.FilterResult(stream, false);
        };
        WebJarResourcesFilter filter2 = (fileName, stream) -> {
            capturedNames[1] = fileName;
            return new WebJarResourcesFilter.FilterResult(stream, false);
        };

        CombinedWebJarResourcesFilter combined = new CombinedWebJarResourcesFilter(List.of(filter1, filter2));
        InputStream input = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

        combined.apply("styles/main.css", input);

        assertEquals("styles/main.css", capturedNames[0]);
        assertEquals("styles/main.css", capturedNames[1]);
    }
}
