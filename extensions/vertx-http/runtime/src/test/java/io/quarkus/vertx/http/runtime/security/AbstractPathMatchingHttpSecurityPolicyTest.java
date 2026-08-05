package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.normalizePath;
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

    @Test
    public void testNormalizePathCleanPathUnchanged() {
        assertEquals("/api/admin/data", normalizePath("/api/admin/data"));
        assertEquals("/", normalizePath("/"));
    }

    @Test
    public void testNormalizePathStripMatrixParams() {
        assertEquals("/api/admin/data", normalizePath("/api/admin;x=1/data"));
        assertEquals("/api/admin/data", normalizePath("/api/admin;bypass=true/data"));
    }

    @Test
    public void testNormalizePathPercentDecoding() {
        assertEquals("/api/admin/data", normalizePath("/api/adm%69n/data"));
        assertEquals("/secret/confidential.html", normalizePath("/secret%2Fconfidential.html"));
        assertEquals("/api/admin/data", normalizePath("/%61pi/%61dmin/d%61t%61"));
    }

    @Test
    public void testNormalizePathDoubleEncoding() {
        assertEquals("/api/admin/data", normalizePath("/api/adm%2569n/data"));
        assertEquals("/secret/confidential.html", normalizePath("/secret%252Fconfidential.html"));
        assertEquals("/secret/confidential.html", normalizePath("/secret%25252Fconfidential.html"));
    }

    @Test
    public void testNormalizePathNullByteRemoval() {
        assertEquals("/api/admin/data", normalizePath("/api/admin\0/data"));
        assertEquals("/api/admin/data", normalizePath("/api/admin%00/data"));
    }

    @Test
    public void testNormalizePathBackslash() {
        assertEquals("/secret/confidential.html", normalizePath("/secret\\confidential.html"));
        assertEquals("/api/admin/data", normalizePath("/api\\admin\\data"));
        assertEquals("/secret/confidential.html", normalizePath("/secret%5Cconfidential.html"));
    }

    @Test
    public void testNormalizePathDotSegmentsAfterDecoding() {
        assertEquals("/etc/passwd", normalizePath("/api/%2e%2e/etc/passwd"));
        assertEquals("/secret.html", normalizePath("/api/../secret.html"));
    }

    @Test
    public void testNormalizePathEncodedSemicolon() {
        assertEquals("/api/baz", normalizePath("/api/baz%3Bv=1.1"));
        assertEquals("/api/admin/data", normalizePath("/api/admin%3Bbypass=true/data"));
    }

    @Test
    public void testNormalizePathCombinedAttacks() {
        assertEquals("/api/admin/data", normalizePath("/api/adm%69n;v=1/data"));
        assertEquals("/secret/file.html", normalizePath("/secret%2Ffile.html\0"));
    }

}
