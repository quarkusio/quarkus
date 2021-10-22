package io.quarkus.openshift.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.kubernetes.deployment.OpenshiftConfig;
import io.quarkus.kubernetes.deployment.ResourceNameUtil;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

public class OpenshiftProcessor {

    @BuildStep
    public void checkOpenshift(ApplicationInfoBuildItem applicationInfo, OpenshiftConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {

        String kind = config.getDepoymentResourceKind();
        String group = config.getDepoymentResourceGroup();
        String version = config.getDepoymentResourceVersion();

        deploymentTargets
                .produce(
                        new KubernetesDeploymentTargetBuildItem(OPENSHIFT, kind, group,
                                version, true));

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        resourceMeta.produce(new KubernetesResourceMetadataBuildItem(OPENSHIFT, group,
                version, kind, name));
    }
}
