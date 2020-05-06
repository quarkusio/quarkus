
package io.quarkus.kubernetes.deployment;

import static io.quarkus.container.image.deployment.util.ImageUtil.hasRegistry;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.deps.kubernetes.api.model.HasMetadata;
import io.dekorate.deps.kubernetes.api.model.KubernetesList;
import io.dekorate.deps.kubernetes.client.KubernetesClient;
import io.dekorate.deps.kubernetes.client.KubernetesClientException;
import io.dekorate.utils.Clients;
import io.dekorate.utils.Serialization;
import io.quarkus.container.image.deployment.ContainerImageCapabilitiesUtil;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.deployment.KubernetesClientErrorHanlder;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;

public class KubernetesDeployer {

    private static final Logger log = Logger.getLogger(KubernetesDeployer.class);
    private static final String CONTAINER_IMAGE_EXTENSIONS_STR = ContainerImageCapabilitiesUtil.CAPABILITY_TO_EXTENSION_NAME
            .values().stream()
            .map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));

    @BuildStep(onlyIf = { IsNormal.class, KubernetesDeploy.class })
    public void deploy(KubernetesClientBuildItem kubernetesClient,
            ContainerImageInfoBuildItem containerImageInfo,
            EnabledKubernetesDeploymentTargetsBuildItem targets,
            OutputTargetBuildItem outputTarget,
            Capabilities capabilities,
            BuildProducer<DeploymentResultBuildItem> deploymentResult,
            // needed to ensure that this step runs after the container image has been built
            @SuppressWarnings("unused") List<ArtifactResultBuildItem> artifactResults) {

        Optional<String> activeContainerImageCapability = ContainerImageCapabilitiesUtil
                .getActiveContainerImageCapability(capabilities);

        if (!activeContainerImageCapability.isPresent()) {
            throw new RuntimeException(
                    "A Kubernetes deployment was requested but no extension was found to build a container image. Consider adding one of following extensions: "
                            + CONTAINER_IMAGE_EXTENSIONS_STR + ".");
        }

        final EnabledKubernetesDeploymentTargetsBuildItem.Entry selectedTarget = determineDeploymentTarget(
                containerImageInfo, targets, activeContainerImageCapability.get());

        final KubernetesClient client = Clients.fromConfig(kubernetesClient.getClient().getConfiguration());
        deploymentResult
                .produce(deploy(selectedTarget, client, outputTarget.getOutputDirectory()));
    }

    /**
     * Determine a single deployment target out of the possible options.
     *
     * The selection is done as follows:
     *
     * If there is no target deployment at all, just use vanilla Kubernetes. This will happen in cases where the user does not
     * select
     * a deployment target and no extensions that specify one are are present
     *
     * If the user specifies deployment targets, pick the first one
     */
    private EnabledKubernetesDeploymentTargetsBuildItem.Entry determineDeploymentTarget(
            ContainerImageInfoBuildItem containerImageInfo,
            EnabledKubernetesDeploymentTargetsBuildItem targets, String activeContainerImageCapability) {
        final EnabledKubernetesDeploymentTargetsBuildItem.Entry selectedTarget;

        boolean checkForMissingRegistry = true;
        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getUserSpecifiedDeploymentTargets();
        if (userSpecifiedDeploymentTargets.isEmpty()) {
            selectedTarget = targets.getEntriesSortedByPriority().get(0);
            if (targets.getEntriesSortedByPriority().size() > 1) {
                log.info("Deploying target '" + selectedTarget.getName()
                        + "' since it has the highest priority among the implicitly enabled deployment targets");
            }
        } else {
            String firstUserSpecifiedDeploymentTarget = userSpecifiedDeploymentTargets.get(0);
            selectedTarget = targets
                    .getEntriesSortedByPriority()
                    .stream()
                    .filter(d -> d.getName().equals(firstUserSpecifiedDeploymentTarget))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("The specified value '" + firstUserSpecifiedDeploymentTarget
                            + "' is not one of the allowed values of \"quarkus.kubernetes.deployment-target\""));
            if (userSpecifiedDeploymentTargets.size() > 1) {
                log.info(
                        "Only the first deployment target (which is '" + firstUserSpecifiedDeploymentTarget
                                + "') selected via \"quarkus.kubernetes.deployment-target\" will be deployed");
            }
        }

        if (OPENSHIFT.equals(selectedTarget.getName())) {
            checkForMissingRegistry = Capabilities.CONTAINER_IMAGE_S2I.equals(activeContainerImageCapability);
        } else if (MINIKUBE.equals(selectedTarget.getName())) {
            checkForMissingRegistry = false;
        }

        if (checkForMissingRegistry && !hasRegistry(containerImageInfo.getImage())) {
            log.warn(
                    "A Kubernetes deployment was requested, but the container image to be built will not be pushed to any registry"
                            + " because \"quarkus.container-image.registry\" has not been set. The Kubernetes deployment will only work properly"
                            + " if the cluster is using the local Docker daemon. For that reason 'ImagePullPolicy' is being force-set to 'IfNotPresent'.");
        }
        return selectedTarget;
    }

    private Optional<KubernetesDeploymentTargetBuildItem> getOptionalDeploymentTarget(
            List<KubernetesDeploymentTargetBuildItem> deploymentTargets, String name) {
        return deploymentTargets.stream().filter(d -> name.equals(d.getName())).findFirst();
    }

    private DeploymentResultBuildItem deploy(EnabledKubernetesDeploymentTargetsBuildItem.Entry deploymentTarget,
            KubernetesClient client, Path outputDir) {
        String namespace = Optional.ofNullable(client.getNamespace()).orElse("default");
        log.info("Deploying to " + deploymentTarget.getName().toLowerCase() + " server: " + client.getMasterUrl()
                + " in namespace: " + namespace + ".");
        File manifest = outputDir.resolve(KUBERNETES).resolve(deploymentTarget.getName().toLowerCase() + ".yml")
                .toFile();

        try (FileInputStream fis = new FileInputStream(manifest)) {
            KubernetesList list = Serialization.unmarshalAsList(fis);
            distinct(list.getItems()).forEach(i -> {
                client.resource(i).inNamespace(namespace).createOrReplace();
                log.info("Applied: " + i.getKind() + " " + i.getMetadata().getName() + ".");
            });

            HasMetadata m = list.getItems().stream().filter(r -> r.getKind().equals(deploymentTarget.getKind()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "No " + deploymentTarget.getKind() + " found under: " + manifest.getAbsolutePath()));
            return new DeploymentResultBuildItem(m.getMetadata().getName(), m.getMetadata().getLabels());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Can't find generated kubernetes manifest: " + manifest.getAbsolutePath());
        } catch (KubernetesClientException e) {
            KubernetesClientErrorHanlder.handle(e);
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Error closing file: " + manifest.getAbsolutePath());
        }

    }

    public static Predicate<HasMetadata> distictByResourceKey() {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(t.getApiVersion() + "/" + t.getKind() + ":" + t.getMetadata().getName(),
                Boolean.TRUE) == null;
    }

    private static Collection<HasMetadata> distinct(Collection<HasMetadata> resources) {
        return resources.stream().filter(distictByResourceKey()).collect(Collectors.toList());
    }
}
