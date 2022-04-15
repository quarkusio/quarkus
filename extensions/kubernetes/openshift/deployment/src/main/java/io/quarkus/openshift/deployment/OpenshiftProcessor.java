package io.quarkus.openshift.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.kubernetes.deployment.OpenshiftConfig;
import io.quarkus.kubernetes.deployment.OpenshiftConfig.DeploymentResourceKind;
import io.quarkus.kubernetes.deployment.ResourceNameUtil;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

public class OpenshiftProcessor {

    @BuildStep
    public void checkOpenshift(ApplicationInfoBuildItem applicationInfo, OpenshiftConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {

        DeploymentResourceKind deploymentResourceKind = config.getDeploymentResourceKind();
        deploymentTargets
                .produce(
                        new KubernetesDeploymentTargetBuildItem(OPENSHIFT, deploymentResourceKind.kind,
                                deploymentResourceKind.apiGroup,
                                deploymentResourceKind.apiVersion, true));

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        resourceMeta.produce(new KubernetesResourceMetadataBuildItem(OPENSHIFT, deploymentResourceKind.apiGroup,
                deploymentResourceKind.apiVersion, deploymentResourceKind.kind, name));
    }
}
