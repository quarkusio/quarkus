package io.quarkus.deployment.steps;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAllowIncompleteClasspathAggregateBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAllowIncompleteClasspathBuildItem;

@SuppressWarnings("deprecation")
public final class NativeImageAllowIncompleteClasspathAggregateStep {

    private static final Logger log = Logger.getLogger(NativeImageAllowIncompleteClasspathAggregateStep.class);

    @BuildStep
    NativeImageAllowIncompleteClasspathAggregateBuildItem aggregateIndividualItems(
            List<NativeImageAllowIncompleteClasspathBuildItem> list) {
        if (list.isEmpty()) {
            return new NativeImageAllowIncompleteClasspathAggregateBuildItem(false);
        } else {
            final String extensionsRequiringBrokenClasspath = list.stream()
                    .map(NativeImageAllowIncompleteClasspathBuildItem::getExtensionName)
                    .collect(Collectors.joining(","));
            log.warn("The following extensions have required the '--allow-incomplete-classpath' flag to be set: {"
                    + extensionsRequiringBrokenClasspath
                    + "}. This is a global flag which might have unexpected effects on other extensions as well, and is a hint of the library "
                    +
                    "needing some additional refactoring to better support GraalVM native-image. In the case of 3rd party dependencies and/or"
                    +
                    " proprietary code there is not much we can do - please ask for support to your library vendor." +
                    " If you incur in any problem with other Quarkus extensions, please try reproducing the problem without these extensions first.");
            return new NativeImageAllowIncompleteClasspathAggregateBuildItem(true);
        }
    }

}
