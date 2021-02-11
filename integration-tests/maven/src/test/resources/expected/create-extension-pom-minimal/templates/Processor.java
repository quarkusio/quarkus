package [=javaPackageBase].deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class [=artifactIdBaseCamelCase]Processor {

    // Custom

    private static final String FEATURE = "[=artifactIdBase]";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

}
