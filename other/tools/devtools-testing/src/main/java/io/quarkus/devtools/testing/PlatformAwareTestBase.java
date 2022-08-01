package io.quarkus.devtools.testing;

import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.getCodestartResourceLoaders;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PlatformAwareTestBase {

    @RegisterExtension
    static final RegistryClientTest registryClientTest = new RegistryClientTest();

    private ExtensionCatalog catalog;
    private Properties quarkusProps;

    protected List<ResourceLoader> getCodestartsResourceLoaders() {
        return getCodestartResourceLoaders(getExtensionsCatalog());
    }

    protected ExtensionCatalog getExtensionsCatalog() {
        if (catalog == null) {
            try {
                catalog = QuarkusProjectHelper.getCatalogResolver().resolveExtensionCatalog();
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve extension catalog", e);
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
