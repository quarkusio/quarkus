package io.quarkus.vertx.http.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpRootPathBuildItemTest {
    @Test
    void testResolvePathWithSlash() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");

        Assertions.assertEquals("/", buildItem.resolvePath(""));
        Assertions.assertEquals("/", buildItem.resolvePath("/"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }

    @Test
    void testResolvePathWithSlashApp() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");

        Assertions.assertEquals("/app", buildItem.resolvePath(""));
        Assertions.assertEquals("/", buildItem.resolvePath("/"));
        Assertions.assertEquals("/app/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
    }

    @Test
    void testResolveResolvedPath() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");
        Assertions.assertEquals("/app", buildItem.resolvePath("/app"));
        Assertions.assertEquals("/app/foo", buildItem.resolvePath("/app/foo"));
    }

    @Test
    void testRelativePathWithSlash() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");

        Assertions.assertEquals("/foo", buildItem.relativePath("foo"));
        Assertions.assertEquals("/foo", buildItem.relativePath("/foo"));
    }

    @Test
    void testRelativePathWithSlashApp() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");

        Assertions.assertEquals("/app/foo", buildItem.relativePath("foo"));
        // Unlike resolvePath, relativePath treats /foo as relative too
        Assertions.assertEquals("/app/foo", buildItem.relativePath("/foo"));
    }

    @Test
    void testRelativePathWithSubPath() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");

        Assertions.assertEquals("/app/foo/bar", buildItem.relativePath("foo/bar"));
        Assertions.assertEquals("/app/foo/bar", buildItem.relativePath("/foo/bar"));
    }

    @Test
    void testGetRootPathWithSlash() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        Assertions.assertEquals("/", buildItem.getRootPath());
    }

    @Test
    void testGetRootPathWithApp() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");
        Assertions.assertEquals("/app/", buildItem.getRootPath());
    }

    @Test
    void testResolvePathWithWildcard() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");
        Assertions.assertEquals("/foo/*", buildItem.resolvePath("foo/*"));
        Assertions.assertEquals("/foo/*", buildItem.resolvePath("/foo/*"));
    }

    @Test
    void testResolvePathWithWildcardAndApp() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");
        Assertions.assertEquals("/app/foo/*", buildItem.resolvePath("foo/*"));
        Assertions.assertEquals("/foo/*", buildItem.resolvePath("/foo/*"));
    }
}
