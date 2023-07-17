package io.quarkus.vertx.http.deployment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpRootPathBuildItemTest {
    @Test
    void testResolvePathWithSlash() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/");

        Assertions.assertEquals("/", buildItem.resolvePath(""));
        Assertions.assertEquals("/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> buildItem.resolvePath("../foo"));
    }

    @Test
    void testResolvePathWithSlashApp() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");

        Assertions.assertEquals("/app", buildItem.resolvePath(""));
        Assertions.assertEquals("/app/foo", buildItem.resolvePath("foo"));
        Assertions.assertEquals("/foo", buildItem.resolvePath("/foo"));
    }

    @Test
    void testResolveResolvedPath() {
        HttpRootPathBuildItem buildItem = new HttpRootPathBuildItem("/app");
        Assertions.assertEquals("/app", buildItem.resolvePath("/app"));
        Assertions.assertEquals("/app/foo", buildItem.resolvePath("/app/foo"));
    }
}
