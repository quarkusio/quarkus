package io.quarkus.platform.tools;

import java.io.IOException;
import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;

public class ToolsUtils {

    public static String requireProperty(String name) {
        final String value = getProperty(name);
        if(value == null) {
            throw new IllegalStateException("Failed to resolve required property " + name);
        }
        return value;
    }

    public static String getProperty(String name) {
        return getProperty(name, null);
    }

    public static String getProperty(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }

    public static boolean isNullOrEmpty(String arg) {
        return arg == null || arg.isEmpty();
    }

    public static String dotJoin(String... parts) {
        if(parts.length == 0) {
            return null;
        }
        if(parts.length == 1) {
            return parts[0];
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(parts[0]);
        int i = 1;
        while(i < parts.length) {
            buf.append('.').append(parts[i++]);
        }
        return buf.toString();
    }

    public static Properties readQuarkusProperties(QuarkusPlatformDescriptor platformDescr) {
        final Properties properties;
        try {
            properties = platformDescr.loadResource("quarkus.properties", is -> {
                final Properties props = new Properties();
                props.load(is);
                return props;
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read quarkus.properties", e);
        }
        return properties;
    }

    public static String getPluginArtifactId(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_PLUGIN_ARTIFACT_ID);
    }

    public static String getPluginGroupId(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_PLUGIN_GROUP_ID);
    }

    public static String getPluginVersion(Properties props) {
        return props.getProperty(ToolsConstants.PROP_QUARKUS_PLUGIN_VERSION);
    }

    public static String getPluginKey(Properties props) {
        return getPluginGroupId(props) + ":" + getPluginArtifactId(props);
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
