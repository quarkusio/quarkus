package io.quarkus.flyway.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@Substitute
@TargetClass(className = "org.flywaydb.core.internal.util.FeatureDetector")
public final class FeatureDetectorSubstitutions {

    @Substitute
    public FeatureDetectorSubstitutions(ClassLoader classLoader) {

    }

    @Substitute
    public boolean isApacheCommonsLoggingAvailable() {
        return false;
    }

    @Substitute
    public boolean isSlf4jAvailable() {
        return false;
    }

    @Substitute
    public boolean isJBossVFSv2Available() {
        return false;
    }

    @Substitute
    public boolean isJBossVFSv3Available() {
        return false;
    }

    @Substitute
    public boolean isOsgiFrameworkAvailable() {
        return false;
    }

    @Substitute
    public boolean isAndroidAvailable() {
        return false;
    }

    @Substitute
    public boolean isLog4J2Available() {
        return false;
    }

    @Substitute
    public static boolean areExperimentalFeaturesEnabled() {
        return false;
    }
}