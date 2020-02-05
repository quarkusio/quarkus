
package io.quarkus.kubernetes.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

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
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;

public class KubernetesDeployer {

    private static final Logger LOG = Logger.getLogger(KubernetesDeployer.class);

    @BuildStep(onlyIf = { IsNormal.class, KubernetesDeploy.class })
    public void deploy(KubernetesClientBuildItem kubernetesClient,
            ApplicationInfoBuildItem applicationInfo,
            Optional<ContainerImageResultBuildItem> containerImage,
            OutputTargetBuildItem outputTarget,
            BuildProducer<DeploymentResultBuildItem> deploymentResult) {

        return kubernetesConfig.getDeploymentTarget().stream().findFirst().map(d -> {
            String namespace = Optional.of(kubernetesClient.getClient().getNamespace()).orElse("default");

            LOG.info("Deploying to " + d.name().toLowerCase() + "in namespace:" + namespace + ".");
            File manifest = outputTarget.getOutputDirectory().resolve("kubernetes").resolve(d.name().toLowerCase() + ".yml")
                    .toFile();
            try (FileInputStream fis = new FileInputStream(manifest)) {
                List<HasMetadata> resources = kubernetesClient.getClient().load(fis).inNamespace(namespace).createOrReplace();
                HasMetadata m = resources.stream()
                        .filter(r -> r.getKind().equals(d.getKind()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No " + d.getKind() + " found under: " + manifest.getAbsolutePath()));
                return new DeploymentResultBuildItem(m.getMetadata().getName(), m.getMetadata().getLabels());
            } catch (FileNotFoundException e) {
                throw new IllegalStateException("Can't find generated kubernetes manifest: " + manifest.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Error closing file: " + manifest.getAbsolutePath());
            }
        }).get();
    }

    public void setKubernetesconfig(KubernetesConfig kubernetesConfig) {
        this.kubernetesConfig = kubernetesConfig;
    }
}
