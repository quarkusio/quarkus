package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.toArtifact;

import java.util.Arrays;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.CapabilityContract;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Extension info used when building an ApplicationModel
 */
class ExtensionInfo {

    final Artifact runtimeArtifact;
    final Properties props;
    final Artifact deploymentArtifact;
    final Artifact[] conditionalDeps;
    final ArtifactKey[] dependencyCondition;
    boolean activated;

    ExtensionInfo() {
        runtimeArtifact = null;
        props = null;
        deploymentArtifact = null;
        conditionalDeps = null;
        dependencyCondition = null;
    }

    ExtensionInfo(Artifact runtimeArtifact, Properties props, boolean devMode) throws BootstrapDependencyProcessingException {
        this.runtimeArtifact = runtimeArtifact;
        this.props = props;
        this.deploymentArtifact = initDeploymentArtifact(props.getProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT),
                runtimeArtifact);
        this.conditionalDeps = initConditionalDeps(devMode);
        dependencyCondition = BootstrapUtils
                .parseDependencyCondition(props.getProperty(BootstrapConstants.DEPENDENCY_CONDITION));
    }

    void ensureActivated(ApplicationModelBuilder appBuilder) {
        if (activated) {
            return;
        }
        activated = true;
        appBuilder.handleExtensionProperties(props, getKey(runtimeArtifact));

        final String providesCapabilities = props.getProperty(BootstrapConstants.PROP_PROVIDES_CAPABILITIES);
        final String requiresCapabilities = props.getProperty(BootstrapConstants.PROP_REQUIRES_CAPABILITIES);
        if (providesCapabilities != null || requiresCapabilities != null) {
            appBuilder.addExtensionCapabilities(
                    CapabilityContract.of(toCompactCoords(runtimeArtifact), providesCapabilities, requiresCapabilities));
        }
    }

    private static Artifact initDeploymentArtifact(String deploymentArtifactStr, Artifact runtimeArtifact)
            throws BootstrapDependencyProcessingException {
        if (deploymentArtifactStr == null) {
            throw new BootstrapDependencyProcessingException("Extension descriptor from " + runtimeArtifact
                    + " does not include " + BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT);
        }
        final Artifact deploymentArtifact = toArtifact(deploymentArtifactStr);
        return deploymentArtifact.getVersion() == null || deploymentArtifact.getVersion().isEmpty()
                ? deploymentArtifact.setVersion(runtimeArtifact.getVersion())
                : deploymentArtifact;
    }

    private Artifact[] initConditionalDeps(boolean devMode) throws BootstrapDependencyProcessingException {
        return devMode
                ? toArtifactArray(
                        joinAndDedupe(splitConditionalDeps(props.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES)),
                                splitConditionalDeps(props.getProperty(BootstrapConstants.CONDITIONAL_DEV_DEPENDENCIES))),
                        runtimeArtifact)
                : toArtifactArray(splitConditionalDeps(props.getProperty(BootstrapConstants.CONDITIONAL_DEPENDENCIES)),
                        runtimeArtifact);
    }

    private static Artifact[] toArtifactArray(String[] strArr, Artifact runtimeArtifact)
            throws BootstrapDependencyProcessingException {
        var artifactArr = new Artifact[strArr.length];
        for (int i = 0; i < strArr.length; ++i) {
            try {
                artifactArr[i] = toArtifact(strArr[i]);
            } catch (Exception e) {
                throw new BootstrapDependencyProcessingException(
                        "Failed to parse conditional dependencies configuration of " + runtimeArtifact, e);
            }
        }
        return artifactArr;
    }

    private static String[] splitConditionalDeps(String conditionalDepsStr) {
        return conditionalDepsStr == null ? new String[0] : BootstrapUtils.splitByWhitespace(conditionalDepsStr);
    }

    private static String[] joinAndDedupe(String[] arr1, String[] arr2) {
        if (arr1.length == 0) {
            return arr2;
        }
        if (arr2.length == 0) {
            return arr1;
        }
        if (arr1.length == 1) {
            var result = new String[arr2.length + 1];
            result[0] = arr1[0];
            for (int i = 0; i < arr2.length; ++i) {
                result[1 + i] = arr2[i];
            }
            return result;
        }
        var result = new String[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, result, 0, arr1.length);
        int resultSize = arr1.length;
        for (int i = 0; i < arr2.length; ++i) {
            var e = arr2[i];
            if (!contains(arr1, e)) {
                result[resultSize++] = e;
            }
        }
        return result.length == resultSize ? result : Arrays.copyOf(result, resultSize);
    }

    private static <T> boolean contains(T[] arr, T item) {
        for (int i = 0; i < arr.length; ++i) {
            if (item.equals(arr[i])) {
                return true;
            }
        }
        return false;
    }

    private static String toCompactCoords(Artifact a) {
        final StringBuilder b = new StringBuilder();
        b.append(a.getGroupId()).append(':').append(a.getArtifactId()).append(':');
        if (!a.getClassifier().isEmpty()) {
            b.append(a.getClassifier()).append(':');
        }
        if (!ArtifactCoords.TYPE_JAR.equals(a.getExtension())) {
            b.append(a.getExtension()).append(':');
        }
        b.append(a.getVersion());
        return b.toString();
    }
}
