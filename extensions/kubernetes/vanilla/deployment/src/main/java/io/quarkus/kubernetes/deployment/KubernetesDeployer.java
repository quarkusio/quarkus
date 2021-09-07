
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.utils.Clients;
import io.dekorate.utils.Serialization;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.container.image.deployment.ContainerImageCapabilitiesUtil;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.deployment.KubernetesClientErrorHandler;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;

public class KubernetesDeployer {

    private static final Logger log = Logger.getLogger(KubernetesDeployer.class);
    private static final String CONTAINER_IMAGE_EXTENSIONS_STR = ContainerImageCapabilitiesUtil.CAPABILITY_TO_EXTENSION_NAME
            .values().stream()
            .map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));

    @BuildStep(onlyIf = IsNormalNotRemoteDev.class)
    public void selectDeploymentTarget(ContainerImageInfoBuildItem containerImageInfo,
            EnabledKubernetesDeploymentTargetsBuildItem targets,
            Capabilities capabilities,
            BuildProducer<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget) {

        Optional<String> activeContainerImageCapability = ContainerImageCapabilitiesUtil
                .getActiveContainerImageCapability(capabilities);

        if (activeContainerImageCapability.isEmpty()) {
            // we can't thrown an exception here, because it could prevent the Kubernetes resources from being generated
            return;
        }

        final DeploymentTargetEntry selectedTarget = determineDeploymentTarget(
                containerImageInfo, targets, activeContainerImageCapability.get());
        selectedDeploymentTarget.produce(new SelectedKubernetesDeploymentTargetBuildItem(selectedTarget));
    }

    @BuildStep(onlyIf = IsNormalNotRemoteDev.class)
    public void deploy(KubernetesClientBuildItem kubernetesClient,
            Capabilities capabilities,
            Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            OutputTargetBuildItem outputTarget,
            OpenshiftConfig openshiftConfig,
            ApplicationInfoBuildItem applicationInfo,
            BuildProducer<DeploymentResultBuildItem> deploymentResult,
            // needed to ensure that this step runs after the container image has been built
            @SuppressWarnings("unused") List<ArtifactResultBuildItem> artifactResults) {

        if (!KubernetesDeploy.INSTANCE.check()) {
            return;
        }

        if (selectedDeploymentTarget.isEmpty()) {

            if (ContainerImageCapabilitiesUtil
                    .getActiveContainerImageCapability(capabilities).isEmpty()) {
                throw new RuntimeException(
                        "A Kubernetes deployment was requested but no extension was found to build a container image. Consider adding one of following extensions: "
                                + CONTAINER_IMAGE_EXTENSIONS_STR + ".");
            }

            return;
        }

        final KubernetesClient client = Clients.fromConfig(kubernetesClient.getClient().getConfiguration());
        deploymentResult
                .produce(deploy(selectedDeploymentTarget.get().getEntry(), client, outputTarget.getOutputDirectory(),
                        openshiftConfig, applicationInfo));
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
    private DeploymentTargetEntry determineDeploymentTarget(
            ContainerImageInfoBuildItem containerImageInfo,
            EnabledKubernetesDeploymentTargetsBuildItem targets, String activeContainerImageCapability) {
        final DeploymentTargetEntry selectedTarget;

        boolean checkForMissingRegistry = true;
        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getUserSpecifiedDeploymentTargets();
        if (userSpecifiedDeploymentTargets.isEmpty()) {
            selectedTarget = targets.getEntriesSortedByPriority().get(0);
            if (targets.getEntriesSortedByPriority().size() > 1) {
                log.info("Selecting target '" + selectedTarget.getName()
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
            checkForMissingRegistry = Capability.CONTAINER_IMAGE_S2I.equals(activeContainerImageCapability);
        } else if (MINIKUBE.equals(selectedTarget.getName())) {
            checkForMissingRegistry = false;
        }

        if (checkForMissingRegistry && !containerImageInfo.getRegistry().isPresent()) {
            log.warn(
                    "A Kubernetes deployment was requested, but the container image to be built will not be pushed to any registry"
                            + " because \"quarkus.container-image.registry\" has not been set. The Kubernetes deployment will only work properly"
                            + " if the cluster is using the local Docker daemon. For that reason 'ImagePullPolicy' is being force-set to 'IfNotPresent'.");
        }
        return selectedTarget;
    }

    private DeploymentResultBuildItem deploy(DeploymentTargetEntry deploymentTarget,
            KubernetesClient client, Path outputDir,
            OpenshiftConfig openshiftConfig, ApplicationInfoBuildItem applicationInfo) {
        String namespace = Optional.ofNullable(client.getNamespace()).orElse("default");
        log.info("Deploying to " + deploymentTarget.getName().toLowerCase() + " server: " + client.getMasterUrl()
                + " in namespace: " + namespace + ".");
        File manifest = outputDir.resolve(KUBERNETES).resolve(deploymentTarget.getName().toLowerCase() + ".yml")
                .toFile();

        try (FileInputStream fis = new FileInputStream(manifest)) {
            KubernetesList list = Serialization.unmarshalAsList(fis);
            list.getItems().stream().filter(distinctByResourceKey()).forEach(i -> {
                final var r = client.resource(i).inNamespace(namespace);
                if (shouldDeleteExisting(deploymentTarget, i)) {
                    r.delete();
                    r.waitUntilCondition(Objects::isNull, 10, TimeUnit.SECONDS);
                }
                r.createOrReplace();
                log.info("Applied: " + i.getKind() + " " + i.getMetadata().getName() + ".");
            });

            printExposeInformation(client, list, openshiftConfig, applicationInfo);

            HasMetadata m = list.getItems().stream().filter(r -> r.getKind().equals(deploymentTarget.getKind()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "No " + deploymentTarget.getKind() + " found under: " + manifest.getAbsolutePath()));
            return new DeploymentResultBuildItem(m.getMetadata().getName(), m.getMetadata().getLabels());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Can't find generated kubernetes manifest: " + manifest.getAbsolutePath());
        } catch (KubernetesClientException e) {
            KubernetesClientErrorHandler.handle(e);
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Error closing file: " + manifest.getAbsolutePath());
        }

    }

    private void printExposeInformation(KubernetesClient client, KubernetesList list, OpenshiftConfig openshiftConfig,
            ApplicationInfoBuildItem applicationInfo) {
        String generatedRouteName = ResourceNameUtil.getResourceName(openshiftConfig, applicationInfo);
        List<HasMetadata> items = list.getItems();
        for (HasMetadata item : items) {
            if (Constants.ROUTE_API_GROUP.equals(item.getApiVersion()) && Constants.ROUTE.equals(item.getKind())
                    && generatedRouteName.equals(item.getMetadata().getName())) {
                try {
                    OpenShiftClient openShiftClient = client.adapt(OpenShiftClient.class);
                    Route route = openShiftClient.routes().withName(generatedRouteName).get();
                    boolean isTLS = (route.getSpec().getTls() != null);
                    String host = route.getSpec().getHost();
                    log.infov("The deployed application can be accessed at: http{0}://{1}", isTLS ? "s" : "", host);
                } catch (KubernetesClientException ignored) {

                }
                break;
            }
        }
    }

    private static boolean shouldDeleteExisting(DeploymentTargetEntry deploymentTarget, HasMetadata resource) {
        return KNATIVE.equalsIgnoreCase(deploymentTarget.getName())
                || resource instanceof Service
                || (Objects.equals("v1", resource.getApiVersion()) && Objects.equals("Service", resource.getKind()));
    }

    private static Predicate<HasMetadata> distinctByResourceKey() {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(t.getApiVersion() + "/" + t.getKind() + ":" + t.getMetadata().getName(),
                Boolean.TRUE) == null;
    }
}
