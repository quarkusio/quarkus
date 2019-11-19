
package io.quarkus.kubernetes.deployment;

import org.jboss.logging.Logger;

import io.dekorate.deps.kubernetes.api.model.HasMetadata;
import io.dekorate.deps.kubernetes.api.model.KubernetesList;
import io.dekorate.deps.kubernetes.client.KubernetesClient;
import io.dekorate.utils.Clients;
import io.dekorate.utils.Serialization;
import io.quarkus.container.spi.ContainerImageResultBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.ContainerImageResultBuildItem;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.ExecUtil;

public class KubernetesDeployer {

    private static final Logger LOG = Logger.getLogger(KubernetesDeployer.class);

    @BuildStep(onlyIf = { IsNormal.class, KubernetesDeploy.class })
    public void deploy(KubernetesClientBuildItem kubernetesClient,
            ApplicationInfoBuildItem applicationInfo,
            Optional<ContainerImageResultBuildItem> containerImage,
            OutputTargetBuildItem outputTarget,
            BuildProducer<DeploymentResultBuildItem> deploymentResult) {

        kubernetesConfig.getDeploymentTarget().stream().map(Enum::name).map(String::toLowerCase).findFirst().ifPresent(d -> {
            LOG.info("Deploying to " + d + ".");
            ExecUtil.exec(outputTarget.getOutputDirectory().toFile(), "oc", "apply", "-f",
                    "kubernetes/" + d + ".yml");
        });

        return new DeploymentResultBuildItem(null, null);
    }

    public void setKubernetesconfig(KubernetesConfig kubernetesConfig) {
        this.kubernetesConfig = kubernetesConfig;
    }
}
