package io.quarkus.arc.deployment;

import io.quarkus.arc.runtime.CommandLineParametersProducer;
import io.quarkus.deployment.annotations.BuildStep;

/**
 * Command line parameters bean related build steps.
 */
public class CommandLineBeanBuildStep {
    @BuildStep
    AdditionalBeanBuildItem bean() {
        return new AdditionalBeanBuildItem(CommandLineParametersProducer.class);
    }
}
