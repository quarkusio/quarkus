package io.quarkus.devtools;

import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.ToolsUtils;

public class PlatformAwareTestBase {

    private QuarkusPlatformDescriptor platformDescr;
    private Properties quarkusProps;

    protected QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr == null
                ? platformDescr = QuarkusJsonPlatformDescriptorResolver.newInstance().resolveBundled()
                : platformDescr;
    }

    private Properties getQuarkusProperties() {
        if (quarkusProps == null) {
            quarkusProps = ToolsUtils.readQuarkusProperties(getPlatformDescriptor());
        }
        return quarkusProps;
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
        return getPlatformDescriptor().getBomGroupId();
    }

    protected String getBomArtifactId() {
        return getPlatformDescriptor().getBomArtifactId();
    }

    protected String getBomVersion() {
        return getPlatformDescriptor().getBomVersion();
    }
}
