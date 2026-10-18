package io.quarkus.deployment.util;

import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UriNormalizationTest {
    @Test
    void testToUri() {
        Assertions.assertEquals("/", UriNormalizationUtil.toURI("/", true).getPath());
        Assertions.assertEquals("", UriNormalizationUtil.toURI("/", false).getPath());

        Assertions.assertEquals("/", UriNormalizationUtil.toURI("./", true).getPath());
        Assertions.assertEquals("", UriNormalizationUtil.toURI("./", false).getPath());

        Assertions.assertEquals("bob/", UriNormalizationUtil.toURI("bob/", true).getPath());
        Assertions.assertEquals("bob", UriNormalizationUtil.toURI("bob/", false).getPath());
    }

    @Test
    void testExamples() {
        URI root = UriNormalizationUtil.toURI("/", true);
        URI prefix = UriNormalizationUtil.toURI("/prefix", true);
        URI foo = UriNormalizationUtil.toURI("foo", true);

        Assertions.assertEquals("/example/", UriNormalizationUtil.normalizeWithBase(root, "example", true).getPath());
        Assertions.assertEquals("/example", UriNormalizationUtil.normalizeWithBase(root, "example", false).getPath());
        Assertions.assertEquals("/example/", UriNormalizationUtil.normalizeWithBase(root, "/example", true).getPath());
        Assertions.assertEquals("/example", UriNormalizationUtil.normalizeWithBase(root, "/example", false).getPath());

        Assertions.assertEquals("/prefix/example/", UriNormalizationUtil.normalizeWithBase(prefix, "example", true).getPath());
        Assertions.assertEquals("/prefix/example", UriNormalizationUtil.normalizeWithBase(prefix, "example", false).getPath());
        Assertions.assertEquals("/example/", UriNormalizationUtil.normalizeWithBase(prefix, "/example", true).getPath());
        Assertions.assertEquals("/example", UriNormalizationUtil.normalizeWithBase(prefix, "/example", false).getPath());

        Assertions.assertEquals("foo/example/", UriNormalizationUtil.normalizeWithBase(foo, "example", true).getPath());
        Assertions.assertEquals("foo/example", UriNormalizationUtil.normalizeWithBase(foo, "example", false).getPath());
        Assertions.assertEquals("/example/", UriNormalizationUtil.normalizeWithBase(foo, "/example", true).getPath());
        Assertions.assertEquals("/example", UriNormalizationUtil.normalizeWithBase(foo, "/example", false).getPath());
    }

    @Test
    void testDubiousUriPaths() {
        URI root = UriNormalizationUtil.toURI("/", true);

        Assertions.assertEquals("/", UriNormalizationUtil.normalizeWithBase(root, "#example", false).getPath());
        Assertions.assertEquals("/", UriNormalizationUtil.normalizeWithBase(root, "?example", false).getPath());

        Assertions.assertEquals("/example", UriNormalizationUtil.normalizeWithBase(root, "./example", false).getPath());
        Assertions.assertEquals("/example", UriNormalizationUtil.normalizeWithBase(root, "//example", false).getPath());

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "/%2fexample", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "junk/../example", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "junk/../../example", false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> UriNormalizationUtil.normalizeWithBase(root, "../example", false));
    }
}
