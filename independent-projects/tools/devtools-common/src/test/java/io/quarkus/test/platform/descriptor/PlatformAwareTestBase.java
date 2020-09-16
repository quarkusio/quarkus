package io.quarkus.test.platform.descriptor;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public class PlatformAwareTestBase {

    private QuarkusPlatformDescriptor platformDescr;

    protected QuarkusPlatformDescriptor getPlatformDescriptor() {
        return platformDescr == null ? platformDescr = QuarkusPlatformConfig.builder().build().getPlatformDescriptor()
                : platformDescr;
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
