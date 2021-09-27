package io.quarkus.maven.it;

import java.util.Properties;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;

public class QuarkusPlatformAwareMojoTestBase extends MojoTestBase {

    private ExtensionCatalog catalog;
    private Properties quarkusProps;

    private ExtensionCatalog getPlatformDescriptor() {
        if (catalog == null) {
            enableDevToolsTestConfig(System.getProperties());
            try {
                catalog = QuarkusProjectHelper.getCatalogResolver().resolveExtensionCatalog();
            } catch (RegistryResolutionException e) {
                throw new RuntimeException("Failed to resolve the extension catalog", e);
            } finally {
                disableDevToolsTestConfig(System.getProperties());
            }
        }
        return catalog;
    }

    private Properties getQuarkusProperties() {
        return quarkusProps == null ? quarkusProps = ToolsUtils.readQuarkusProperties(getPlatformDescriptor()) : quarkusProps;
    }

    protected String getMavenPluginGroupId() {
        return ToolsUtils.getMavenPluginGroupId(getQuarkusProperties());
    }

    protected String getMavenPluginArtifactId() {
        return ToolsUtils.getMavenPluginArtifactId(getQuarkusProperties());
    }

    protected String getMavenPluginVersion() {
        return ToolsUtils.getMavenPluginVersion(getQuarkusProperties());
    }

    protected String getQuarkusCoreVersion() {
        return ToolsUtils.getQuarkusCoreVersion(getQuarkusProperties());
    }

    protected String getBomGroupId() {
        return getPlatformDescriptor().getBom().getGroupId();
    }

    protected String getBomArtifactId() {
        return getPlatformDescriptor().getBom().getArtifactId();
    }

    protected String getBomVersion() {
        return getPlatformDescriptor().getBom().getVersion();
    }
}
