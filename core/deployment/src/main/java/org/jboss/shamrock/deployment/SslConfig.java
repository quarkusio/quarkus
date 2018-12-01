package org.jboss.shamrock.deployment;

import java.io.File;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.SystemPropertyBuildItem;

public class SslConfig {
    /**
     * Enable support for SSL on the resulting native binary
     */
    @ConfigProperty(name = "shamrock.ssl.native", defaultValue = "false")
    boolean enableSsl;
    
    @BuildStep
    SystemPropertyBuildItem setupNativeSsl() {
        String graalVmHome = System.getenv("GRAALVM_HOME");
        if(enableSsl) {
            // I assume we only fail if we actually enable it, but perhaps there's a no-native called that we can't
            // see here?
            
            // FIXME: fail build? what sort of error here?
            if(graalVmHome == null)
                throw new RuntimeException("GRAALVM_HOME environment variable required");
            return new SystemPropertyBuildItem("java.library.path", graalVmHome+File.separator+"jre"+File.separator+"lib"+File.separator+"amd64");
        }
        return null;
    }
}
