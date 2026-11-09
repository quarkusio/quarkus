package io.quarkus.deployment.pkg.steps;

import io.quarkus.bootstrap.runner.CracSupport;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;

/**
 * Makes the build time value of {@code quarkus.package.jar.crac.enabled} visible to the application.
 */
public class CracProcessor {

    /**
     * {@link CracSupport} reads a system property when the application runs. The application sets this system property at
     * startup when the build time property is {@code true}.
     *
     * @param packageConfig the packaging configuration
     * @param systemProperties the system properties that the application sets at startup
     */
    @BuildStep(onlyIfNot = NativeOrNativeSourcesBuild.class)
    void enableCracSupport(PackageConfig packageConfig, BuildProducer<SystemPropertyBuildItem> systemProperties) {
        if (packageConfig.jar().crac().enabled()) {
            systemProperties.produce(new SystemPropertyBuildItem(CracSupport.ENABLED_PROPERTY, "true"));
        }
    }
}
