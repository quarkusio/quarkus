package io.quarkus.platform.tools;

import java.util.List;

/**
 * The parts of a Quarkus extension checked by {@link ExtensionStructureChecker}: its coordinates and the classes of
 * its runtime and deployment modules.
 *
 * @param groupId the group ID of the runtime artifact
 * @param runtimeArtifactId the artifact ID of the runtime module
 * @param deploymentArtifactId the artifact ID of the deployment module
 * @param runtimeClasses the classes of the runtime module
 * @param deploymentClasses the classes of the deployment module
 */
public record ExtensionStructure(String groupId, String runtimeArtifactId, String deploymentArtifactId,
        List<ClassFacts> runtimeClasses, List<ClassFacts> deploymentClasses) {

    public ExtensionStructure {
        runtimeClasses = List.copyOf(runtimeClasses);
        deploymentClasses = List.copyOf(deploymentClasses);
    }

    /**
     * What the structure rules need to know about a class.
     *
     * @param name the fully qualified class name
     * @param declaresBuildSteps whether the class declares {@code @BuildStep} methods
     * @param nativeImageHook whether the class is a GraalVM substitution or feature
     */
    public record ClassFacts(String name, boolean declaresBuildSteps, boolean nativeImageHook) {

        String packageName() {
            int lastDot = name.lastIndexOf('.');
            return lastDot < 0 ? "" : name.substring(0, lastDot);
        }

        String simpleName() {
            return name.substring(name.lastIndexOf('.') + 1);
        }
    }
}
