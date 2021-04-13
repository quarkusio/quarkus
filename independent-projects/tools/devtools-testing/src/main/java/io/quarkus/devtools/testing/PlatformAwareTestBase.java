package io.quarkus.devtools.testing;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class PlatformAwareTestBase {

    private ExtensionCatalog catalog;
    private Properties quarkusProps;

    @BeforeAll
    static void enableDevToolsTestConfig() {
        RegistryClientTestHelper.enableRegistryClientTestConfig();
    }

    @AfterAll
    static void disableDevToolsTestConfig() {
        RegistryClientTestHelper.disableRegistryClientTestConfig();
    }

    protected List<ResourceLoader> getCodestartsResourceLoaders() {
        return QuarkusProjectHelper.getCodestartResourceLoaders(getExtensionsCatalog());
    }

    protected ExtensionCatalog getExtensionsCatalog() {
        if (catalog == null) {
            try {
                catalog = QuarkusProjectHelper.getCatalogResolver().resolveExtensionCatalog();
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve extensions catalog", e);
            }
        }
        return catalog;
    }

    protected Properties getQuarkusProperties() {
        return quarkusProps == null ? quarkusProps = ToolsUtils.readQuarkusProperties(getExtensionsCatalog()) : quarkusProps;
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

    protected String getBomVersion() {
        return getQuarkusCoreVersion();
    }
}
