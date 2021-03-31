package io.quarkus.devtools;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.BOM_ARTIFACT_ID;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.BOM_GROUP_ID;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.BOM_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.JAVA_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.KOTLIN_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.MAVEN_COMPILER_PLUGIN_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.MAVEN_SUREFIRE_PLUGIN_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.PROJECT_ARTIFACT_ID;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.PROJECT_GROUP_ID;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.PROJECT_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.QUARKUS_GRADLE_PLUGIN_ID;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.QUARKUS_GRADLE_PLUGIN_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.QUARKUS_MAVEN_PLUGIN_GROUP_ID;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.QUARKUS_MAVEN_PLUGIN_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.QUARKUS_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.SCALA_MAVEN_PLUGIN_VERSION;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.SCALA_VERSION;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.test.RegistryClientTestHelper;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.ExtensionCatalog;

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

    protected Map<String, Object> getTestInputData(ExtensionCatalog catalog, Map<String, Object> override) {

        final ArtifactCoords bom = catalog.getBom();

        final HashMap<String, Object> data = new HashMap<>();
        final Properties quarkusProp = getQuarkusProperties();
        data.put(PROJECT_GROUP_ID.key(), "org.test");
        data.put(PROJECT_ARTIFACT_ID.key(), "test-codestart");
        data.put(PROJECT_VERSION.key(), "1.0.0-codestart");
        data.put(BOM_GROUP_ID.key(), bom.getGroupId());
        data.put(BOM_ARTIFACT_ID.key(), bom.getArtifactId());
        data.put(BOM_VERSION.key(), bom.getVersion());
        data.put(QUARKUS_VERSION.key(), catalog.getQuarkusCoreVersion());
        data.put(QUARKUS_MAVEN_PLUGIN_GROUP_ID.key(), "io.quarkus");
        data.put(QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID.key(), "quarkus-maven-plugin");
        data.put(QUARKUS_MAVEN_PLUGIN_VERSION.key(), catalog.getQuarkusCoreVersion());
        data.put(QUARKUS_GRADLE_PLUGIN_ID.key(), "io.quarkus");
        data.put(QUARKUS_GRADLE_PLUGIN_VERSION.key(), catalog.getQuarkusCoreVersion());
        data.put(KOTLIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_KOTLIN_VERSION));
        data.put(SCALA_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_SCALA_VERSION));
        data.put(SCALA_MAVEN_PLUGIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_SCALA_PLUGIN_VERSION));
        data.put(MAVEN_COMPILER_PLUGIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_COMPILER_PLUGIN_VERSION));
        data.put(MAVEN_SUREFIRE_PLUGIN_VERSION.key(), quarkusProp.getProperty(ToolsConstants.PROP_SUREFIRE_PLUGIN_VERSION));
        data.put(JAVA_VERSION.key(), "11");
        if (override != null)
            data.putAll(override);
        return data;
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
