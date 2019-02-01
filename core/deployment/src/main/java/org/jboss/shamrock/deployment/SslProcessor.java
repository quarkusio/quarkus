package org.jboss.shamrock.deployment;

import java.io.File;

import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.builditem.SystemPropertyBuildItem;
import org.jboss.shamrock.runtime.annotations.ConfigGroup;
import org.jboss.shamrock.runtime.annotations.ConfigItem;

public class SslProcessor {
    /**
     * SSL configuration.
     */
    Config ssl;

    @ConfigGroup
    static class Config {
        /**
         * Enable native SSL support.
         */
        @ConfigItem(name = "native")
        boolean native_;
    }

    @BuildStep
    SystemPropertyBuildItem setupNativeSsl() {
        String graalVmHome = System.getenv("GRAALVM_HOME");
        if(ssl.native_) {
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
