
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOY;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_TARGET;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
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

        Config config = ConfigProvider.getConfig();
        if (!config.getOptionalValue(DEPLOY, Boolean.class).orElse(false)) {
            return;
        }

        List<DeploymentTarget> deploymentTargets = Arrays
                .stream(config.getOptionalValue(DEPLOYMENT_TARGET, String.class).orElse(KUBERNETES).split(","))
                .map(String::trim).map(String::toUpperCase).map(DeploymentTarget::valueOf).collect(Collectors.toList());

        final KubernetesClient client = Clients.fromConfig(kubernetesClient.getClient().getConfiguration());
        deploymentResult.produce(deploymentTargets.stream().findFirst().map(d -> {
            String namespace = Optional.ofNullable(kubernetesClient.getClient().getNamespace()).orElse("default");

            LOG.info("Deploying to " + d.name().toLowerCase() + " in namespace:" + namespace + ".");
            File manifest = outputTarget.getOutputDirectory().resolve("kubernetes")
                    .resolve(d.name().toLowerCase() + ".yml").toFile();
            try (FileInputStream fis = new FileInputStream(manifest)) {
                KubernetesList list = Serialization.unmarshalAsList(fis);
                list.getItems().forEach(i -> {
                    LOG.info("Creating: " + i.getApiVersion() + "/" + i.getKind() + " name:" + i.getMetadata().getName() + ".");
                    client.resource(i).inNamespace(namespace).createOrReplace();
                    LOG.info("Created: " + i.getKind() + " name:" + i.getMetadata().getName() + ".");
                });

                HasMetadata m = list.getItems().stream().filter(r -> r.getKind().equals(d.getKind())).findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "No " + d.getKind() + " found under: " + manifest.getAbsolutePath()));

                return new DeploymentResultBuildItem(m.getMetadata().getName(), m.getMetadata().getLabels());
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(
                        "Can't find generated kubernetes manifest: " + manifest.getAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("Error closing file: " + manifest.getAbsolutePath());
            }
        }).get());
    }
}
