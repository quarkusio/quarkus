package io.quarkus.container.image.openshift.deployment;

import static io.quarkus.container.image.openshift.deployment.OpenshiftProcessor.concatUnixPaths;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OpenshiftProcessorTest {
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
