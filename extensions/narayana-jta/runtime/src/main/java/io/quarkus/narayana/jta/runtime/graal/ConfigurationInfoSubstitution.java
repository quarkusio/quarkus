package io.quarkus.narayana.jta.runtime.graal;

import com.arjuna.common.util.ConfigurationInfo;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ConfigurationInfo.class)
final class ConfigurationInfoSubstitution {

    // initialize build time properties from data in the jar's META-INF/MANIFEST.MF
    //TODO: actually implement this somehow so these values are baked in at build time

    @Substitute
    public static String getSourceId() {
        return "a66d5";
    }

    @Substitute
    public static String getPropertiesFile() {
        return "jbossts-properties.xml";
    }

    @Substitute
    public static String getBuildId() {
        return "JBoss Inc. [ochaloup] Linux 5.1.20-300.fc30.x86_64 201";
    }
}
