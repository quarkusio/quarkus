package org.jboss.shamrock.transactions.runtime.graal;

import com.arjuna.common.util.ConfigurationInfo;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ConfigurationInfo.class)
final class ConfigurationInfoSubstitution {

    @Alias
    private static String sourceId = "unknown";
    @Alias
    private static String propertiesFile = "arjuna-properties.xml";
    @Alias
    private static String buildId = "arjuna-builder";
    @Alias
    private static boolean isInitialized = false;


    // initialize build time properties from data in the jar's META-INF/MANIFEST.MF
    //TODO: actually implement this somehow so these values are baked in at build time
    @Substitute
    private static synchronized void getBuildTimeProperties() {
        if (isInitialized) {
            return;
        }
        propertiesFile = "jbossts-properties.xml";
        sourceId = "4d505";
        buildId = "JBoss Inc. [ochaloup] Linux 4.15.4-200.fc26.x86_64 2018/Feb/26 19:35";
        isInitialized = true;
    }
}
