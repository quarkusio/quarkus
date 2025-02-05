package io.quarkus.vertx.http.deployment;

import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.runtime.management.ManagementAuthConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.core.http.ClientAuth;

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
        ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = new ManagementBuildTimeConfigImpl(true,
                "management");
        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q",
                managementBuildTimeConfig.rootPath());
        Assertions.assertEquals("/management/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://localhost:9000/management/foo",
                buildItem.resolveManagementPath("foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/management/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo",
                buildItem.resolveManagementPath("/foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithRelativeRootPathInTestMode() {
        ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = new ManagementBuildTimeConfigImpl(true,
                "management");
        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), true);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q",
                managementBuildTimeConfig.rootPath());
        Assertions.assertEquals("/management/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://localhost:9001/management/foo",
                buildItem.resolveManagementPath("foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9001/management/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9001/foo",
                buildItem.resolveManagementPath("/foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9001/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithRelativeRootPathAndWithManagementDisabled() {
        ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = new ManagementBuildTimeConfigImpl(
                false, "management");
        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "q", null);

        Assertions.assertEquals("/q/", buildItem.getManagementRootPath());
        Assertions.assertEquals("/q/foo",
                buildItem.resolveManagementPath("foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("/q/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("/foo",
                buildItem.resolveManagementPath("/foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithAbsoluteRootPath() {
        ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = new ManagementBuildTimeConfigImpl(true,
                "/management");
        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q",
                managementBuildTimeConfig.rootPath());
        Assertions.assertEquals("/management/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://localhost:9000/management/foo",
                buildItem.resolveManagementPath("foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/management/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo",
                buildItem.resolveManagementPath("/foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithEmptyRootPath() {
        ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = new ManagementBuildTimeConfigImpl(true,
                "");
        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q",
                managementBuildTimeConfig.rootPath());
        Assertions.assertEquals("/", buildItem.getManagementRootPath());
        Assertions.assertEquals("http://localhost:9000/foo",
                buildItem.resolveManagementPath("foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo/sub/path",
                buildItem.resolveManagementPath("foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo",
                buildItem.resolveManagementPath("/foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo/sub/path",
                buildItem.resolveManagementPath("/foo/sub/path", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("../foo", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> buildItem.resolveManagementPath("", managementBuildTimeConfig, launchModeBuildItem));
    }

    @Test
    void testResolveManagementPathWithWithWildcards() {
        ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = new ManagementBuildTimeConfigImpl(true,
                "/management");
        LaunchModeBuildItem launchModeBuildItem = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        NonApplicationRootPathBuildItem buildItem = new NonApplicationRootPathBuildItem("/", "/q",
                managementBuildTimeConfig.rootPath());
        Assertions.assertEquals("http://localhost:9000/management/foo/*",
                buildItem.resolveManagementPath("foo/*", managementBuildTimeConfig, launchModeBuildItem));
        Assertions.assertEquals("http://localhost:9000/foo/*",
                buildItem.resolveManagementPath("/foo/*", managementBuildTimeConfig, launchModeBuildItem));
    }

    private static final class ManagementBuildTimeConfigImpl implements ManagementInterfaceBuildTimeConfig {
        private final boolean enabled;
        private final String rootPath;

        public ManagementBuildTimeConfigImpl(final boolean enabled, final String rootPath) {
            this.enabled = enabled;
            this.rootPath = rootPath;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public ManagementAuthConfig auth() {
            return null;
        }

        @Override
        public ClientAuth tlsClientAuth() {
            return null;
        }

        @Override
        public String rootPath() {
            return rootPath;
        }

        @Override
        public boolean enableCompression() {
            return false;
        }

        @Override
        public boolean enableDecompression() {
            return false;
        }

        @Override
        public OptionalInt compressionLevel() {
            return OptionalInt.empty();
        }
    }
}
