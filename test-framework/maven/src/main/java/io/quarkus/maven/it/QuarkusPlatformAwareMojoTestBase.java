package io.quarkus.maven.it;

import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.ToolsUtils;

public class QuarkusPlatformAwareMojoTestBase extends MojoTestBase {

    private QuarkusPlatformDescriptor platformDescr;
    private Properties quarkusProps;
    private String pluginGroupId;
    private String pluginArtifactId;
    private String pluginVersion;

    protected QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr == null ? platformDescr = QuarkusJsonPlatformDescriptorResolver.newInstance().resolveBundled()
                : platformDescr;
    }

    private Properties getQuarkusProperties() {
        if (quarkusProps == null) {
            quarkusProps = ToolsUtils.readQuarkusProperties(getPlatformDescriptor());
        }
        return quarkusProps;
    }

    protected String getPluginGroupId() {
        return pluginGroupId == null ? pluginGroupId = getQuarkusProperties().getProperty("plugin-groupId") : pluginGroupId;
    }

    protected String getPluginArtifactId() {
        return pluginArtifactId == null ? pluginArtifactId = getQuarkusProperties().getProperty("plugin-artifactId")
                : pluginArtifactId;
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
