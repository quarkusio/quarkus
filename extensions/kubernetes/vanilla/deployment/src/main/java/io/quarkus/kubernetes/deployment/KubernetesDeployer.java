
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KIND;
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
import io.fabric8.kubernetes.api.model.APIResourceList;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.ApiVersionUtil;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.container.image.deployment.ContainerImageCapabilitiesUtil;
import io.quarkus.container.image.deployment.ContainerImageConfig;
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
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentClusterBuildItem;
import io.quarkus.kubernetes.spi.KubernetesOptionalResourceDefinitionBuildItem;

public class KubernetesDeployer {

    private static final Logger log = Logger.getLogger(KubernetesDeployer.class);
    private static final String CONTAINER_IMAGE_EXTENSIONS_STR = ContainerImageCapabilitiesUtil.CAPABILITY_TO_EXTENSION_NAME
            .values().stream()
            .map(s -> "\"" + s + "\"").collect(Collectors.joining(", "));

    @BuildStep(onlyIf = IsNormalNotRemoteDev.class)
    public void selectDeploymentTarget(ContainerImageInfoBuildItem containerImageInfo,
            EnabledKubernetesDeploymentTargetsBuildItem targets,
            Capabilities capabilities,
            ContainerImageConfig containerImageConfig,
            BuildProducer<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            BuildProducer<PreventImplicitContainerImagePushBuildItem> preventImplicitContainerImagePush) {

        Optional<String> activeContainerImageCapability = ContainerImageCapabilitiesUtil
                .getActiveContainerImageCapability(capabilities);

        if (activeContainerImageCapability.isEmpty()) {
            // we can't throw an exception here, because it could prevent the Kubernetes resources from being generated
            return;
        }

        final DeploymentTargetEntry selectedTarget = determineDeploymentTarget(containerImageInfo, targets,
                activeContainerImageCapability.get(), containerImageConfig);
        selectedDeploymentTarget.produce(new SelectedKubernetesDeploymentTargetBuildItem(selectedTarget));
        if (MINIKUBE.equals(selectedTarget.getName()) || KIND.equals(selectedTarget.getName())) {
            preventImplicitContainerImagePush.produce(new PreventImplicitContainerImagePushBuildItem());
        }
    }

    @BuildStep
    public void checkEnvironment(Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            KubernetesClientBuildItem client,
            List<GeneratedKubernetesResourceBuildItem> resources,
            BuildProducer<KubernetesDeploymentClusterBuildItem> deploymentCluster) {

        if (!KubernetesDeploy.INSTANCE.checkSilently()) {
            return;
        }
        String target = selectedDeploymentTarget.map(s -> s.getEntry().getName()).orElse(KUBERNETES);
        if (target.equals(KUBERNETES)) {
            deploymentCluster.produce(new KubernetesDeploymentClusterBuildItem(KUBERNETES));
        }
    }

    @BuildStep(onlyIf = IsNormalNotRemoteDev.class)
    public void deploy(KubernetesClientBuildItem kubernetesClient,
            Capabilities capabilities,
            List<KubernetesDeploymentClusterBuildItem> deploymentClusters,
            Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            OutputTargetBuildItem outputTarget,
            OpenshiftConfig openshiftConfig,
            ApplicationInfoBuildItem applicationInfo,
            List<KubernetesOptionalResourceDefinitionBuildItem> optionalResourceDefinitions,
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
                        openshiftConfig, applicationInfo, optionalResourceDefinitions));
    }

    /**
     * Determine a single deployment target out of the possible options.
     *
     * The selection is done as follows:
     *
     * If there is no target deployment at all, just use vanilla Kubernetes. This will happen in cases where the user does not
     * select a deployment target and no extension that specify one is present.
     *
     * If the user specifies deployment targets, pick the first one.
     */
    private DeploymentTargetEntry determineDeploymentTarget(
            ContainerImageInfoBuildItem containerImageInfo,
            EnabledKubernetesDeploymentTargetsBuildItem targets, String activeContainerImageCapability,
            ContainerImageConfig containerImageConfig) {
        final DeploymentTargetEntry selectedTarget;

        boolean checkForMissingRegistry = true;
        boolean checkForNamespaceGroupAlignment = false;
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
            checkForMissingRegistry = Capability.CONTAINER_IMAGE_S2I.equals(activeContainerImageCapability)
                    || Capability.CONTAINER_IMAGE_OPENSHIFT.equals(activeContainerImageCapability);

            // We should ensure that we have image group and namespace alignment we are not using deployment triggers via DeploymentConfig.
            if (!targets.getEntriesSortedByPriority().get(0).getKind().equals("DeploymentConfig")) {
                checkForNamespaceGroupAlignment = true;
            }

        } else if (MINIKUBE.equals(selectedTarget.getName()) || KIND.equals(selectedTarget.getName())) {
            checkForMissingRegistry = false;
        } else if (containerImageConfig.isPushExplicitlyDisabled()) {
            checkForMissingRegistry = false;
        }

        if (checkForMissingRegistry && !containerImageInfo.getRegistry().isPresent()) {
            log.warn(
                    "A Kubernetes deployment was requested, but the container image to be built will not be pushed to any registry"
                            + " because \"quarkus.container-image.registry\" has not been set. The Kubernetes deployment will only work properly"
                            + " if the cluster is using the local Docker daemon. For that reason 'ImagePullPolicy' is being force-set to 'IfNotPresent'.");
        }

        //This might also be applicable in other scenarios too (e.g. Knative on Openshift), so we might need to make it slightly more generic.
        if (checkForNamespaceGroupAlignment) {
            Config config = Config.autoConfigure(null);
            if (config.getNamespace() != null && !config.getNamespace().equals(containerImageInfo.getGroup())) {
                log.warn("An openshift deployment was requested, but the container image group:" + containerImageInfo.getGroup()
                        + " is not aligned with the currently selected project:" + config.getNamespace() + "."
                        + "it is strongly advised to align them, or else the image might not be reachable.");
            }
        }
        return selectedTarget;
    }

    private DeploymentResultBuildItem deploy(DeploymentTargetEntry deploymentTarget,
            KubernetesClient client, Path outputDir,
            OpenshiftConfig openshiftConfig, ApplicationInfoBuildItem applicationInfo,
            List<KubernetesOptionalResourceDefinitionBuildItem> optionalResourceDefinitions) {
        String namespace = Optional.ofNullable(client.getNamespace()).orElse("default");
        log.info("Deploying to " + deploymentTarget.getName().toLowerCase() + " server: " + client.getMasterUrl()
                + " in namespace: " + namespace + ".");
        File manifest = outputDir.resolve(KUBERNETES).resolve(deploymentTarget.getName().toLowerCase() + ".yml")
                .toFile();

        try (FileInputStream fis = new FileInputStream(manifest)) {
            KubernetesList list = Serialization.unmarshalAsList(fis);
            list.getItems().stream().filter(distinctByResourceKey()).forEach(i -> {
                if (i instanceof GenericKubernetesResource) {
                    GenericKubernetesResource genericResource = (GenericKubernetesResource) i;
                    ResourceDefinitionContext context = getGenericResourceContext(client, genericResource)
                            .orElseThrow(() -> new IllegalStateException("Could not retrieve API resource information for:"
                                    + i.getApiVersion() + " " + i.getKind() + ". Is the CRD for the resource available?"));

                    client.genericKubernetesResources(context).resource(genericResource).createOrReplace();
                } else {
                    final var r = client.resource(i);
                    if (shouldDeleteExisting(deploymentTarget, i)) {
                        r.delete();
                        try {
                            r.waitUntilCondition(Objects::isNull, 10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            if (e instanceof InterruptedException) {
                                throw e;
                            }
                            //This is something that should not really happen. it's not a fatal condition so let's just log.
                            log.warn("Failed to wait for the deletion of: " + i.getApiVersion() + " " + i.getKind() + " "
                                    + i.getMetadata().getName() + ". Is the resource waitable?");
                        }
                    }
                    try {
                        r.createOrReplace();
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            throw e;
                        } else if (isOptional(optionalResourceDefinitions, i)) {
                            log.warn("Failed to apply: " + i.getKind() + " " + i.getMetadata().getName()
                                    + ", possibly due to missing a CRD apiVersion: " + i.getApiVersion() + " and kind: "
                                    + i.getKind() + ".");
                        } else {
                            throw e;
                        }
                    }
                }
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

    /**
     * Obtain everything the APIResourceList from the server and extract all the info we need in order to know how to create /
     * delete the specified generic resource.
     *
     * @param client the client instance to use to query the server.
     * @param resource the generic resource.
     * @return an optional {@link ResourceDefinitionContext} with the resource info or empty if resource could not be matched.
     */
    private static Optional<ResourceDefinitionContext> getGenericResourceContext(KubernetesClient client,
            GenericKubernetesResource resource) {
        APIResourceList apiResourceList = client.getApiResources(resource.getApiVersion());
        if (apiResourceList == null || apiResourceList.getResources() == null || apiResourceList.getResources().isEmpty()) {
            return Optional.empty();
        }
        return client.getApiResources(resource.getApiVersion()).getResources().stream()
                .filter(r -> r.getKind().equals(resource.getKind()))
                .map(r -> new ResourceDefinitionContext.Builder()
                        .withGroup(ApiVersionUtil.trimGroup(resource.getApiVersion()))
                        .withVersion(ApiVersionUtil.trimVersion(resource.getApiVersion()))
                        .withKind(r.getKind())
                        .withNamespaced(r.getNamespaced())
                        .withPlural(r.getName())
                        .build())
                .findFirst();
    }

    private static boolean isOptional(List<KubernetesOptionalResourceDefinitionBuildItem> optionalResourceDefinitions,
            HasMetadata resource) {
        return optionalResourceDefinitions.stream()
                .anyMatch(t -> t.getApiVersion().equals(resource.getApiVersion()) && t.getKind().equals(resource.getKind()));
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
