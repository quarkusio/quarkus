package io.quarkus.cli.commands;

import java.io.IOException;
import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public class PlatformAwareTestBase {

    private QuarkusPlatformDescriptor platformDescr;
    private Properties quarkusProps;
    private String pluginGroupId;
    private String pluginArtifactId;
    private String pluginVersion;

    protected QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr == null ? platformDescr = QuarkusPlatformConfig.builder().build().getPlatformDescriptor()
                : platformDescr;
    }

    private Properties getQuarkusProperties() {
        if(quarkusProps == null) {
            try {
                quarkusProps = getPlatformDescriptor().loadResource("quarkus.properties", is -> {
                    final Properties props = new Properties();
                    props.load(is);
                    return props;
                });
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load quarkus.properties", e);
            }
        }
        return quarkusProps;
    }

    protected String getPluginGroupId() {
        return pluginGroupId == null ? pluginGroupId = getQuarkusProperties().getProperty("plugin-groupId") : pluginGroupId;
    }

    protected String getPluginArtifactId() {
        return pluginArtifactId == null ? pluginArtifactId = getQuarkusProperties().getProperty("plugin-artifactId") : pluginArtifactId;
    }

    protected String getPluginVersion() {
        return pluginVersion == null ? pluginVersion = getQuarkusProperties().getProperty("plugin-version") : pluginVersion;
    }

    protected String getBomGroupId() {
        return getPlatformDescriptor().getBomGroupId();
    }

    protected String getBomArtifactId() {
        return getPlatformDescriptor().getBomArtifactId();
    }

    protected String getBomVersion() {
        return getPlatformDescriptor().getBomVersion();
    }
}
