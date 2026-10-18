package io.quarkus.vertx.http.deployment.devmode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.management.ManagementAuthConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.core.http.ClientAuth;

class ConfiguredPathInfoTest {

    @Test
    void getEndpointPath_absolutePath_returnsAsIs() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/absolute/path", true, false);
        HttpRootPathBuildItem httpRoot = new HttpRootPathBuildItem("/app");

        assertEquals("/absolute/path", info.getEndpointPath(httpRoot));
    }

    @Test
    void getEndpointPath_relativePath_adjustedToRoot() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/relative", false, false);
        HttpRootPathBuildItem httpRoot = new HttpRootPathBuildItem("/");

        String result = info.getEndpointPath(httpRoot);
        assertEquals("/relative", result);
    }

    @Test
    void getEndpointPath_relativePath_adjustedToAppRoot() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/endpoint", false, false);
        HttpRootPathBuildItem httpRoot = new HttpRootPathBuildItem("/app");

        String result = info.getEndpointPath(httpRoot);
        // TemplateHtmlBuilder.adjustRoot prepends the root path
        assertEquals("/app/endpoint", result);
    }

    @Test
    void getName_returnsConfiguredName() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("my.config.key", "/path", false, false);
        assertEquals("my.config.key", info.getName());
    }

    @Test
    void getEndpointPath_nonApp_absolutePath_returnsAsIs() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/absolute/path", true, false);
        NonApplicationRootPathBuildItem nonAppRoot = new NonApplicationRootPathBuildItem("/", "q", null);
        ManagementInterfaceBuildTimeConfig mgmtConfig = new TestManagementConfig(false, "management");
        LaunchModeBuildItem launchMode = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        assertEquals("/absolute/path", info.getEndpointPath(nonAppRoot, mgmtConfig, launchMode));
    }

    @Test
    void getEndpointPath_nonApp_relativePath_managementDisabled() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/endpoint", false, false);
        NonApplicationRootPathBuildItem nonAppRoot = new NonApplicationRootPathBuildItem("/", "q", null);
        ManagementInterfaceBuildTimeConfig mgmtConfig = new TestManagementConfig(false, "management");
        LaunchModeBuildItem launchMode = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        String result = info.getEndpointPath(nonAppRoot, mgmtConfig, launchMode);
        // adjustRoot with "/" root path
        assertEquals("/endpoint", result);
    }

    @Test
    void getEndpointPath_nonApp_managementEnabled_managementFlag() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/health", false, true);
        NonApplicationRootPathBuildItem nonAppRoot = new NonApplicationRootPathBuildItem("/", "q", "management");
        ManagementInterfaceBuildTimeConfig mgmtConfig = new TestManagementConfig(true, "management");
        LaunchModeBuildItem launchMode = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        String result = info.getEndpointPath(nonAppRoot, mgmtConfig, launchMode);
        // Should include management URL prefix
        assertEquals("http://localhost:9000/health", result);
    }

    @Test
    void getEndpointPath_nonApp_managementEnabled_notManagementRoute() {
        // management is enabled but this route is NOT a management route
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/endpoint", false, false);
        NonApplicationRootPathBuildItem nonAppRoot = new NonApplicationRootPathBuildItem("/", "q", "management");
        ManagementInterfaceBuildTimeConfig mgmtConfig = new TestManagementConfig(true, "management");
        LaunchModeBuildItem launchMode = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), false);

        String result = info.getEndpointPath(nonAppRoot, mgmtConfig, launchMode);
        // Not a management route, so should use normal path resolution
        assertEquals("/endpoint", result);
    }

    @Test
    void getEndpointPath_nonApp_managementEnabled_testMode() {
        ConfiguredPathInfo info = new ConfiguredPathInfo("test", "/health", false, true);
        NonApplicationRootPathBuildItem nonAppRoot = new NonApplicationRootPathBuildItem("/", "q", "management");
        ManagementInterfaceBuildTimeConfig mgmtConfig = new TestManagementConfig(true, "management");
        LaunchModeBuildItem launchMode = new LaunchModeBuildItem(LaunchMode.NORMAL, Optional.empty(), false,
                Optional.empty(), true);

        String result = info.getEndpointPath(nonAppRoot, mgmtConfig, launchMode);
        // Test mode uses port 9001
        assertEquals("http://localhost:9001/health", result);
    }

    private static final class TestManagementConfig implements ManagementInterfaceBuildTimeConfig {
        private final boolean enabled;
        private final String rootPath;

        TestManagementConfig(boolean enabled, String rootPath) {
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
