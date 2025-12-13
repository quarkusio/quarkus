package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.S2I;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.container.image.deployment.ContainerImageCapabilitiesUtil;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * OpenShift
 */
@ConfigMapping(prefix = "quarkus.openshift")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface OpenShiftConfig extends PlatformConfiguration {

    @Override
    default String targetPlatformName() {
        return Constants.OPENSHIFT;
    }

    enum OpenshiftFlavor {
        v3,
        v4;
    }

    /**
     * The OpenShift flavor / version to use.
     * Older versions of OpenShift have minor differences in the labels and fields they support.
     * This option allows users to have their manifests automatically aligned to the OpenShift 'flavor' they use.
     */
    @WithDefault("v4")
    OpenshiftFlavor flavor();

    /**
     * The kind of the deployment resource to use.
     * Supported values are 'Deployment', 'StatefulSet', 'Job', 'CronJob' and 'DeploymentConfig'. Defaults to 'DeploymentConfig'
     * if {@code flavor == v3}, or 'Deployment' otherwise.
     * DeploymentConfig is deprecated as of OpenShift 4.14. See https://access.redhat.com/articles/7041372 for details.
     */
    Optional<DeploymentResourceKind> deploymentKind();

    /**
     * The number of desired pods
     */
    @WithDefault("1")
    Integer replicas();

    /**
     * The nodePort to set when serviceType is set to nodePort
     */
    OptionalInt nodePort();

    /**
     * OpenShift route configuration
     */
    RouteConfig route();

    /**
     * If set to true, Quarkus will attempt to deploy the application to the target Kubernetes cluster
     */
    @WithDefault("false")
    boolean deploy();

    static boolean isOpenshiftBuildEnabled(ContainerImageConfig containerImageConfig, Capabilities capabilities) {
        boolean implicitlyEnabled = ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities)
                .filter(c -> c.contains(OPENSHIFT) || c.contains(S2I)).isPresent();
        return containerImageConfig.builder().map(b -> b.equals(OPENSHIFT) || b.equals(S2I)).orElse(implicitlyEnabled);
    }

    default DeploymentResourceKind getDeploymentResourceKind(Capabilities capabilities) {
        if (deploymentKind().isPresent()) {
            return deploymentKind().filter(k -> k.isAvailalbleOn(OPENSHIFT)).get();
        } else if (capabilities.isPresent(Capability.PICOCLI)) {
            return DeploymentResourceKind.Job;
        }
        return (flavor() == OpenshiftFlavor.v3) ? DeploymentResourceKind.DeploymentConfig : DeploymentResourceKind.Deployment;
    }
}
