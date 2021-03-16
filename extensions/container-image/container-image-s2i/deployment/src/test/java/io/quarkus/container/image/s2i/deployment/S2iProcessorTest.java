package io.quarkus.container.image.s2i.deployment;

import static io.quarkus.container.image.s2i.deployment.S2iProcessor.concatUnixPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class S2iProcessorTest {
    @Test
    public void concatUnixPathsTest() {
        assertEquals("foo/bar", concatUnixPaths("foo", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/bar/"));

        assertEquals("foo/bar", concatUnixPaths("foo", "/", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "/bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/", "/bar/"));

        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar/"));

        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "/bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "/bar/"));

        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar/", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar/", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar/", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar/", "/"));
    }
}
