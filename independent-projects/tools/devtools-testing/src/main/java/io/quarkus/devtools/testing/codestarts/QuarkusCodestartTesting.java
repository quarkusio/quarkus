package io.quarkus.devtools.testing.codestarts;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey.*;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartData.QuarkusDataKey;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class QuarkusCodestartTesting {

    public static Collection<String> getPlatformBoms() {
        return Collections.singletonList("io.quarkus:quarkus-mock-bom:999-MOCK");
    }

    public static Map<String, Object> getMockedTestInputData(final Map<String, Object> override) {
        final HashMap<String, Object> data = new HashMap<>();
        data.put(PROJECT_GROUP_ID.key(), "org.test");
        data.put(PROJECT_ARTIFACT_ID.key(), "test-codestart");
        data.put(PROJECT_VERSION.key(), "1.0.0-codestart");
        data.put(BOM_GROUP_ID.key(), "io.quarkus");
        data.put(BOM_ARTIFACT_ID.key(), "quarkus-mock-bom");
        data.put(BOM_VERSION.key(), "999-MOCK");
        data.put(QUARKUS_VERSION.key(), "999-MOCK");
        data.put(QUARKUS_MAVEN_PLUGIN_GROUP_ID.key(), "io.quarkus");
        data.put(QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID.key(), "quarkus-mock-maven-plugin");
        data.put(QUARKUS_MAVEN_PLUGIN_VERSION.key(), "999-MOCK");
        data.put(QUARKUS_GRADLE_PLUGIN_ID.key(), "io.quarkus");
        data.put(QUARKUS_GRADLE_PLUGIN_VERSION.key(), "999-MOCK");
        data.put(JAVA_VERSION.key(), "11");
        data.put(KOTLIN_VERSION.key(), "1.4.28-MOCK");
        data.put(SCALA_VERSION.key(), "2.12.8-MOCK");
        data.put(SCALA_MAVEN_PLUGIN_VERSION.key(), "4.1.1-MOCK");
        data.put(MAVEN_COMPILER_PLUGIN_VERSION.key(), "3.8.1-MOCK");
        data.put(MAVEN_SUREFIRE_PLUGIN_VERSION.key(), "3.0.0-MOCK");
        if (override != null)
            data.putAll(override);
        return data;
    }

    public static Map<String, Object> getRealTestInputData(ExtensionCatalog catalog, Map<String, Object> override) {

        final ArtifactCoords bom = catalog.getBom();

        final HashMap<String, Object> data = new HashMap<>();
        final Properties quarkusProp = getQuarkusProperties(catalog);
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

    public static Collection<String> getBoms(Map<String, Object> inputData) {
        return Collections.singletonList(getBom(inputData));
    }

    private static String getBom(Map<String, Object> inputData) {
        return getRequiredValue(inputData, BOM_GROUP_ID) + ":"
                + getRequiredValue(inputData, BOM_ARTIFACT_ID) + ":" + getRequiredValue(inputData, BOM_VERSION);
    }

    private static Object getRequiredValue(Map<String, Object> inputData, QuarkusDataKey key) {
        final Object o = inputData.get(key.key());
        if (o == null) {
            throw new IllegalArgumentException("Required key " + key.key() + " is missing among " + inputData.keySet());
        }
        return o;
    }

    private static Properties getQuarkusProperties(ExtensionCatalog catalog) {
        return ToolsUtils.readQuarkusProperties(catalog);
    }
}
