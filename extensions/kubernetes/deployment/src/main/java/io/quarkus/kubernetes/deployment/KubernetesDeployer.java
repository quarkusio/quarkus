
package io.quarkus.kubernetes.deployment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.SSLHandshakeException;

import org.jboss.logging.Logger;

import io.dekorate.deps.kubernetes.api.model.HasMetadata;
import io.dekorate.deps.kubernetes.api.model.KubernetesList;
import io.dekorate.deps.kubernetes.client.KubernetesClient;
import io.dekorate.deps.kubernetes.client.KubernetesClientException;
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
    public void deploy(KubernetesConfig kubernetesConfig,
            KubernetesClientBuildItem kubernetesClient,
            ApplicationInfoBuildItem applicationInfo,
            Optional<ContainerImageResultBuildItem> containerImage,
            OutputTargetBuildItem outputTarget,
            BuildProducer<DeploymentResultBuildItem> deploymentResult) {

        if (!containerImage.isPresent()) {
            throw new RuntimeException(
                    "A Kubernetes deployment was requested but no extension was found to build a container image. Consider adding one of following extensions: \"quarkus-container-image-jib\", \"quarkus-container-image-docker\" or \"quarkus-container-image-s2i\".");
        }

        Map<String, Object> config = KubernetesConfigUtil.toMap();
        Set<DeploymentTarget> deploymentTargets = new HashSet<>();
        deploymentTargets.addAll(KubernetesConfigUtil.getDeploymentTargets(config).stream()
                .map(String::toUpperCase)
                .map(DeploymentTarget::valueOf)
                .collect(Collectors.toList()));

        deploymentTargets.addAll(kubernetesConfig.deploymentTarget);

        final KubernetesClient client = Clients.fromConfig(kubernetesClient.getClient().getConfiguration());
        DeploymentTarget target = deploymentTargets.stream().findFirst().orElse(DeploymentTarget.KUBERNETES);
        deploymentResult.produce(deploy(target, client, outputTarget.getOutputDirectory()));
    }

    private DeploymentResultBuildItem deploy(DeploymentTarget deploymentTarget, KubernetesClient client, Path outputDir) {
        String namespace = Optional.ofNullable(client.getNamespace()).orElse("default");
        LOG.info("Deploying to " + deploymentTarget.name().toLowerCase() + " server: " + client.getMasterUrl()
                + " in namespace:" + namespace + ".");
        File manifest = outputDir.resolve("kubernetes")
                .resolve(deploymentTarget.name().toLowerCase() + ".yml").toFile();
        try (FileInputStream fis = new FileInputStream(manifest)) {
            KubernetesList list = Serialization.unmarshalAsList(fis);
            list.getItems().forEach(i -> {
                LOG.info("Applying: " + i.getKind() + " " + i.getMetadata().getName() + ".");
                client.resource(i).inNamespace(namespace).createOrReplace();
                LOG.info("Applied: " + i.getKind() + " " + i.getMetadata().getName() + ".");
            });

            HasMetadata m = list.getItems().stream().filter(r -> r.getKind().equals(deploymentTarget.getKind())).findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No " + deploymentTarget.getKind() + " found under: " + manifest.getAbsolutePath()));

            return new DeploymentResultBuildItem(m.getMetadata().getName(), m.getMetadata().getLabels());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(
                    "Can't find generated kubernetes manifest: " + manifest.getAbsolutePath());
        } catch (KubernetesClientException e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                String message = "The application could not be deployed to the cluster because the Kubernetes API Server certificates are not trusted. The certificates can be configured using the relevant configuration propertiers under the 'quarkus.kubernetes-client' config root, or \"quarkus.kubernetes-client.trust-certs=true\" can be set to explicitly trust the certificates (not recommended)";
                LOG.error(message);
                throw new RuntimeException(e.getCause());
            }
            LOG.error("Unable to deploy the application to the Kubernetes cluster: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Error closing file: " + manifest.getAbsolutePath());
        }

    }
}
