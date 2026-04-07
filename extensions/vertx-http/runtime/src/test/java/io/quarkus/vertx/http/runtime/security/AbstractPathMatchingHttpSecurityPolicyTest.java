package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.pathWithoutMatrixParams;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AbstractPathMatchingHttpSecurityPolicyTest {

    @Test
    public void testSinglePathSegmentWithoutMatrixParams() {

        assertEquals("/a", pathWithoutMatrixParams("/a"));
        assertEquals("/a/", pathWithoutMatrixParams("/a/"));
        assertEquals("/", pathWithoutMatrixParams("/"));
        assertEquals("a", pathWithoutMatrixParams("a"));
        assertEquals("", pathWithoutMatrixParams(""));
    }

    @Test
    public void testSinglePathSegmentWithSemicolon() {

        assertEquals("/a", pathWithoutMatrixParams("/a;"));
        assertEquals("/a/", pathWithoutMatrixParams("/a/;"));
        assertEquals("/", pathWithoutMatrixParams("/;"));
        assertEquals("a", pathWithoutMatrixParams("a;"));
        assertEquals("", pathWithoutMatrixParams(";"));
    }

    @Test
    public void testSinglePathSegmentWithMatrixParams() {

        assertEquals("/a", pathWithoutMatrixParams("/a;a1=2"));
        assertEquals("/a/", pathWithoutMatrixParams("/a/;a1=2"));
        assertEquals("/", pathWithoutMatrixParams("/;a1=2"));
        assertEquals("a", pathWithoutMatrixParams("a;a1=2"));

        assertEquals("/a", pathWithoutMatrixParams("/a;a1=2;"));
        assertEquals("/a/", pathWithoutMatrixParams("/a/;a1=2;"));
        assertEquals("/", pathWithoutMatrixParams("/;a1=2;"));
        assertEquals("a", pathWithoutMatrixParams("a;a1=2;"));

    }

    @Test
    public void testTwoPathSegmentsWithoutMatrixParams() {

        assertEquals("/a/b", pathWithoutMatrixParams("/a/b"));
        assertEquals("/a/b/", pathWithoutMatrixParams("/a/b/"));
    }

    @Test
    public void testTwoPathSegmentsWithSemicolon() {
        assertEquals("/a/b", pathWithoutMatrixParams("/a;/b;"));
        assertEquals("/a/b/", pathWithoutMatrixParams("/a;/b;/;"));
        assertEquals("/a/b", pathWithoutMatrixParams("/a/b;"));
        assertEquals("/a/b", pathWithoutMatrixParams("/a;/b"));
        assertEquals("/a/b/", pathWithoutMatrixParams("/a/b/;"));
    }

    @Test
    public void testTwoPathSegmentsWithMatrixParams() {
        assertEquals("/a/b", pathWithoutMatrixParams("/a;k=v/b;k=v"));
        assertEquals("/a/b", pathWithoutMatrixParams("/a;k=v/b"));
        assertEquals("/a/b", pathWithoutMatrixParams("/a/b;k=v"));
        assertEquals("/a/b/", pathWithoutMatrixParams("/a;k=v/b;k=v/"));
    }

    @Test
    public void testThreePathSegmentsWithMatrixParams() {
        assertEquals("/a/b/c", pathWithoutMatrixParams("/a;1/b;2/c;3"));
        assertEquals("/a/b/c", pathWithoutMatrixParams("/a/b;m/c"));
        assertEquals("/a/b/c/", pathWithoutMatrixParams("/a;1/b;2/c;3/"));
    }

    @Test
    public void testTrailingSlashPreservedWithMatrixParams() {
        assertEquals("/a/b/", pathWithoutMatrixParams("/a;m/b;m/"));
        assertEquals("/api/baz/", pathWithoutMatrixParams("/api;x/baz;y/"));
        assertEquals("/a/", pathWithoutMatrixParams("/a;m/"));
    }

    @Test
    public void testMultipleConsecutiveSlashesWithMatrixParams() {
        assertEquals("//a//b", pathWithoutMatrixParams("//a;m//b;m"));
        assertEquals("///a/b", pathWithoutMatrixParams("///a;m/b;m"));
        assertEquals("////api/baz", pathWithoutMatrixParams("////api;x/baz;y"));
    }

    @Test
    public void testMatrixParamWithSpecialValues() {
        assertEquals("/a/b", pathWithoutMatrixParams("/a;k=v=w/b"));
        assertEquals("/a/b", pathWithoutMatrixParams("/a;k=v;k2=v2/b"));
        assertEquals("/a/b", pathWithoutMatrixParams("/a;k=v;k2=v2/b;k3=v3;k4=v4"));
    }

}
