package io.quarkus.vertx.http.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NonApplicationRootPathBuildItemTest {
    @Test
    void testResolvePathWithSlashRelativeQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q");
        Assertions.assertTrue(buildItem.isDedicatedRouterRequired());
        Assertions.assertTrue(buildItem.attachedToMainRouter);
        Assertions.assertEquals("/q/", buildItem.getVertxRouterPath());
        Assertions.assertEquals("/", buildItem.httpRootPath.getPath());
        Assertions.assertEquals("/q/", buildItem.nonApplicationRootPath.getPath());
        Assertions.assertNotEquals(buildItem.httpRootPath, buildItem.nonApplicationRootPath);

        Assertions.assertEquals("/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/q/foo/sub/path", buildItem.resolvePath("foo/sub/path"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertEquals("/foo/sub/path", buildItem.resolvePath("/foo/sub/path"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath(""));
    }

    @Test
    void testResolvePathWithSlashAbsoluteQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q");
        Assertions.assertTrue(buildItem.isDedicatedRouterRequired());
        Assertions.assertTrue(buildItem.attachedToMainRouter);
        Assertions.assertEquals("/q/", buildItem.getVertxRouterPath());
        Assertions.assertEquals("/", buildItem.httpRootPath.getPath());
        Assertions.assertEquals("/q/", buildItem.nonApplicationRootPath.getPath());
        Assertions.assertNotEquals(buildItem.httpRootPath, buildItem.nonApplicationRootPath);

        Assertions.assertEquals("/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
    }

    @Test
    void testResolvePathWithSlashAppWithRelativeQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "q");
        Assertions.assertTrue(buildItem.isDedicatedRouterRequired());
        Assertions.assertTrue(buildItem.attachedToMainRouter);
        Assertions.assertEquals("/q/", buildItem.getVertxRouterPath());
        Assertions.assertEquals("/app/", buildItem.httpRootPath.getPath());
        Assertions.assertEquals("/app/q/", buildItem.nonApplicationRootPath.getPath());
        Assertions.assertNotEquals(buildItem.httpRootPath, buildItem.nonApplicationRootPath);

        Assertions.assertEquals("/app/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
    }

    @Test
    void testResolvePathWithSlashAppWithAbsoluteQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "/q");
        Assertions.assertTrue(buildItem.isDedicatedRouterRequired());
        Assertions.assertFalse(buildItem.attachedToMainRouter);
        Assertions.assertEquals("/q/", buildItem.getVertxRouterPath());
        Assertions.assertEquals("/app/", buildItem.httpRootPath.getPath());
        Assertions.assertEquals("/q/", buildItem.nonApplicationRootPath.getPath());
        Assertions.assertNotEquals(buildItem.httpRootPath, buildItem.nonApplicationRootPath);

        Assertions.assertEquals("/q/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
    }

    @Test
    void testResolvePathWithSlashEmpty() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "");
        Assertions.assertFalse(buildItem.isDedicatedRouterRequired());
        Assertions.assertTrue(buildItem.attachedToMainRouter);
        Assertions.assertEquals("/", buildItem.getVertxRouterPath());
        Assertions.assertEquals("/", buildItem.httpRootPath.getPath());
        Assertions.assertEquals("/", buildItem.nonApplicationRootPath.getPath());
        Assertions.assertEquals(buildItem.httpRootPath, buildItem.nonApplicationRootPath);

        Assertions.assertEquals("/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
    }

    @Test
    void testResolvePathWithSlashWithSlash() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/");
        Assertions.assertFalse(buildItem.isDedicatedRouterRequired());
        Assertions.assertTrue(buildItem.attachedToMainRouter);
        Assertions.assertEquals("/", buildItem.getVertxRouterPath());
        Assertions.assertEquals("/", buildItem.httpRootPath.getPath());
        Assertions.assertEquals("/", buildItem.nonApplicationRootPath.getPath());
        Assertions.assertEquals(buildItem.httpRootPath, buildItem.nonApplicationRootPath);

        Assertions.assertEquals("/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
    }

    @Test
    void testResolvePathWithSlashWithSlashQWithWildcards() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q");
        Assertions.assertEquals("/q/foo/*", buildItem.resolvePath("foo/*"));
        Assertions.assertEquals("/foo/*", buildItem.resolvePath("/foo/*"));
    }
}
