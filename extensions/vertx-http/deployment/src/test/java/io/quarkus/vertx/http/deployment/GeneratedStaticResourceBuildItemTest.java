package io.quarkus.vertx.http.deployment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;

class GeneratedStaticResourceBuildItemTest {

    @Test
    void contentBased_validEndpoint() {
        byte[] content = "hello".getBytes();
        GeneratedStaticResourceBuildItem item = new GeneratedStaticResourceBuildItem("/test.html", content);

        assertEquals("/test.html", item.getEndpoint());
        assertArrayEquals(content, item.getContent());
        assertFalse(item.isFile());
        assertNull(item.getFile());
    }

    @Test
    void fileBased_validEndpoint(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.html");
        Files.writeString(file, "content");

        GeneratedStaticResourceBuildItem item = new GeneratedStaticResourceBuildItem("/test.html", file);

        assertEquals("/test.html", item.getEndpoint());
        assertTrue(item.isFile());
        assertNotNull(item.getFile());
        assertNull(item.getContent());
        assertTrue(item.getFileAbsolutePath().endsWith("test.html"));
    }

    @Test
    void nullEndpoint_throws() {
        assertThrows(NullPointerException.class,
                () -> new GeneratedStaticResourceBuildItem(null, "hello".getBytes()));
    }

    @Test
    void endpointWithoutLeadingSlash_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedStaticResourceBuildItem("test.html", "hello".getBytes()));
    }

    @Test
    void endpointWithTrailingSlash_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedStaticResourceBuildItem("/test/", "hello".getBytes()));
    }

    @Test
    void endpointWithNestedPath() {
        GeneratedStaticResourceBuildItem item = new GeneratedStaticResourceBuildItem(
                "/path/to/resource.js", "content".getBytes());

        assertEquals("/path/to/resource.js", item.getEndpoint());
    }
}
