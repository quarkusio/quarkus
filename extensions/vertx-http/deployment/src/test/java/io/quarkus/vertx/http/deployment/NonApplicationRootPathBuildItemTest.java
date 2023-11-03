package io.quarkus.vertx.http.deployment;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

public class NonApplicationRootPathBuildItemTest {

    @Test
    void testResolvePathWithSlashRelativeQ() {
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q", null);
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
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q", null);
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
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "q", null);
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
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/app", "/q", null);
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
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "", null);
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
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/", null);
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
        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q", null);
        Assertions.assertEquals("/q/foo/*", buildItem.resolvePath("foo/*"));
        Assertions.assertEquals("/foo/*", buildItem.resolvePath("/foo/*"));
    }

    @Test
    void testResolveManagementPathWithRelativeRootPath() {
        ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig = new ManagementInterfaceBuildTimeConfig();
        managementInterfaceBuildTimeConfig.enabled = true;
        managementInterfaceBuildTimeConfig.rootPath = "management";

        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q",
                managementInterfaceBuildTimeConfig.rootPath);
        Assertions.assertEquals("/management/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://0.0.0.0:9000/management/foo",
                buildItem.resolveManagementPath("foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/management/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo",
                buildItem.resolveManagementPath("/foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementInterfaceBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithRelativeRootPathInTestMode() {
        ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig = new ManagementInterfaceBuildTimeConfig();
        managementInterfaceBuildTimeConfig.enabled = true;
        managementInterfaceBuildTimeConfig.rootPath = "management";

        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), true);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q",
                managementInterfaceBuildTimeConfig.rootPath);
        Assertions.assertEquals("/management/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://0.0.0.0:9001/management/foo",
                buildItem.resolveManagementPath("foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9001/management/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9001/foo",
                buildItem.resolveManagementPath("/foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9001/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementInterfaceBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithRelativeRootPathAndWithManagementDisabled() {
        ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig = new ManagementInterfaceBuildTimeConfig();
        managementInterfaceBuildTimeConfig.enabled = false;
        managementInterfaceBuildTimeConfig.rootPath = "management";

        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q", null);

        Assertions.assertEquals("/q/", buildItem.getManagementRootPath());
        Assertions.assertEquals("/q/foo",
                buildItem.resolveManagementPath("foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("/q/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("/foo",
                buildItem.resolveManagementPath("/foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementInterfaceBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithAbsoluteRootPath() {
        ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig = new ManagementInterfaceBuildTimeConfig();
        managementInterfaceBuildTimeConfig.enabled = true;
        managementInterfaceBuildTimeConfig.rootPath = "/management";

        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q",
                managementInterfaceBuildTimeConfig.rootPath);
        Assertions.assertEquals("/management/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://0.0.0.0:9000/management/foo",
                buildItem.resolveManagementPath("foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/management/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo",
                buildItem.resolveManagementPath("/foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementInterfaceBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithEmptyRootPath() {
        ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig = new ManagementInterfaceBuildTimeConfig();
        managementInterfaceBuildTimeConfig.enabled = true;
        managementInterfaceBuildTimeConfig.rootPath = "";

        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q",
                managementInterfaceBuildTimeConfig.rootPath);
        Assertions.assertEquals("/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://0.0.0.0:9000/foo",
                buildItem.resolveManagementPath("foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo",
                buildItem.resolveManagementPath("/foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementInterfaceBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithWithWildcards() {
        ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig = new ManagementInterfaceBuildTimeConfig();
        managementInterfaceBuildTimeConfig.enabled = true;
        managementInterfaceBuildTimeConfig.rootPath = "/management";

        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q",
                managementInterfaceBuildTimeConfig.rootPath);
        Assertions.assertEquals("http://0.0.0.0:9000/management/foo/*",
                buildItem.resolveManagementPath("foo/*", managementInterfaceBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://0.0.0.0:9000/foo/*",
                buildItem.resolveManagementPath("/foo/*", managementInterfaceBuildTimeConfig, launchModeBuildItem));
    }
}
