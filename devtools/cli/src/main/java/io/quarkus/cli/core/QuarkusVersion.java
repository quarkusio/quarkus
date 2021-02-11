package io.quarkus.cli.core;

import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import picocli.CommandLine;

public class QuarkusVersion implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor().getQuarkusVersion() };
    }
}
