package io.quarkus.platform.tools;

import io.quarkus.registry.catalog.ExtensionCatalog;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

public class ToolsUtils {

    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    public static String getProperty(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }

    public static Map<String, String> stringToMap(
            String str, String entrySeparator, String keyValueSeparator) {
        HashMap<String, String> result = new HashMap<>();
        for (String entry : StringUtils.splitByWholeSeparator(str, entrySeparator)) {
            String[] pair = StringUtils.splitByWholeSeparator(entry, keyValueSeparator, 2);

            if (pair.length > 0 && StringUtils.isBlank(pair[0])) {
                throw new IllegalArgumentException("Entry with empty key " + entry);
            }

            switch (pair.length) {
                case 1:
                    result.put(pair[0].trim(), "");
                    break;
                case 2:
                    result.put(pair[0].trim(), pair[1].trim());
                    break;
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static Properties readQuarkusProperties(ExtensionCatalog catalog) {
        Map<Object, Object> map = (Map<Object, Object>) catalog.getMetadata().getOrDefault("project", Collections.emptyMap());
        map = (Map<Object, Object>) map.getOrDefault("properties", Collections.emptyMap());
        final Properties properties = new Properties();
        map.entrySet().forEach(
                e -> properties.setProperty(e.getKey().toString(), e.getValue() == null ? null : e.getValue().toString()));
        return properties;
    }

    public static String requireProperty(Properties props, String name) {
        final String value = props.getProperty(name);
        if (value == null) {
            throw new IllegalStateException("Failed to resolve required property " + name);
        }
        return value;
    }

    public static String getMavenPluginArtifactId(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_MAVEN_PLUGIN_ARTIFACT_ID);
    }

    public static String getMavenPluginGroupId(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_MAVEN_PLUGIN_GROUP_ID);
    }

    public static String getQuarkusCoreVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_CORE_VERSION);
    }

    public static String requireQuarkusCoreVersion(Properties props) {
        return requireProperty(props, ToolsConstants.PROP_QUARKUS_CORE_VERSION);
    }

    public static String getMavenPluginVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_MAVEN_PLUGIN_VERSION);
    }

    public static String getGradlePluginVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_GRADLE_PLUGIN_VERSION);
    }

    public static String getPluginKey(Properties props) {
        return getMavenPluginGroupId(props) + ":" + getMavenPluginArtifactId(props);
    }

    public static String getProposedMavenVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_PROPOSED_MVN_VERSION);
    }

    public static String getMavenWrapperVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_MVN_WRAPPER_VERSION);
    }

    public static String getGradleWrapperVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_GRADLE_WRAPPER_VERSION);
    }
}
