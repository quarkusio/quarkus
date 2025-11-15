
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KIND;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;
import static io.quarkus.kubernetes.deployment.Constants.VERSION_LABEL;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.dekorate.utils.Serialization;
import io.fabric8.kubernetes.api.builder.Visitor;
import io.fabric8.kubernetes.api.model.APIResourceList;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorFluent;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.ApiVersionUtil;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.container.image.deployment.ContainerImageCapabilitiesUtil;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.DeploymentResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.deployment.internal.KubernetesClientErrorHandler;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.DeployStrategy;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentClusterBuildItem;
import io.quarkus.kubernetes.spi.KubernetesOptionalResourceDefinitionBuildItem;
import io.quarkus.kubernetes.spi.KubernetesOutputDirectoryBuildItem;

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

        //If container image actions are explicitly disabled block deployment even if a container image capability is not present.
        if (!containerImageConfig.isBuildExplicitlyDisabled() && !containerImageConfig.isPushExplicitlyDisabled()
                && activeContainerImageCapability.isEmpty()) {
            // we can't throw an exception here, because it could prevent the Kubernetes resources from being generated
            return;
        }

        final DeploymentTargetEntry selectedTarget = determineDeploymentTarget(containerImageInfo, targets,
                containerImageConfig);
        selectedDeploymentTarget.produce(new SelectedKubernetesDeploymentTargetBuildItem(selectedTarget));
        if (MINIKUBE.equals(selectedTarget.getName()) || KIND.equals(selectedTarget.getName())) {
            preventImplicitContainerImagePush.produce(new PreventImplicitContainerImagePushBuildItem());
        }
    }

    @BuildStep
    public void checkEnvironment(Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            KubernetesClientBuildItem kubernetesClientBuilder,
            List<GeneratedKubernetesResourceBuildItem> resources,
            BuildProducer<KubernetesDeploymentClusterBuildItem> deploymentCluster) {

        if (!KubernetesDeploy.INSTANCE.checkSilently(kubernetesClientBuilder)) {
            return;
        }
        String target = selectedDeploymentTarget.map(s -> s.getEntry().getName()).orElse(KUBERNETES);
        if (target.equals(KUBERNETES)) {
            deploymentCluster.produce(new KubernetesDeploymentClusterBuildItem(KUBERNETES));
        }
    }

    @BuildStep(onlyIf = IsNormalNotRemoteDev.class)
    public void deploy(KubernetesClientBuildItem kubernetesClientBuilder,
            Capabilities capabilities,
            List<KubernetesDeploymentClusterBuildItem> deploymentClusters,
            Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            OutputTargetBuildItem outputTarget,
            KubernetesOutputDirectoryBuildItem outputDirectoryBuildItem,
            OpenShiftConfig openshiftConfig,
            ContainerImageConfig containerImageConfig,
            ApplicationInfoBuildItem applicationInfo,
            List<KubernetesOptionalResourceDefinitionBuildItem> optionalResourceDefinitions,
            BuildProducer<DeploymentResultBuildItem> deploymentResult,
            // needed to ensure that this step runs after the container image has been built
            @SuppressWarnings("unused") List<ArtifactResultBuildItem> artifactResults) {

        if (!KubernetesDeploy.INSTANCE.check(kubernetesClientBuilder)) {
            return;
        }

        if (selectedDeploymentTarget.isEmpty()) {

            if (!containerImageConfig.isBuildExplicitlyDisabled() &&
                    !containerImageConfig.isPushExplicitlyDisabled() &&
                    ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities).isEmpty()) {
                throw new RuntimeException(
                        "A Kubernetes deployment was requested but no extension was found to build a container image. Consider adding one of following extensions: "
                                + CONTAINER_IMAGE_EXTENSIONS_STR + ".");
            }

            return;
        }

        try (final KubernetesClient client = kubernetesClientBuilder.buildClient()) {
            deploymentResult
                    .produce(deploy(selectedDeploymentTarget.get().getEntry(), client,
                            outputDirectoryBuildItem.getOutputDirectory(),
                            openshiftConfig, applicationInfo, optionalResourceDefinitions));
        }
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
            EnabledKubernetesDeploymentTargetsBuildItem targets,
            ContainerImageConfig containerImageConfig) {
        final DeploymentTargetEntry selectedTarget;

        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getExplicitlyConfiguredDeploymentTargets();
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

        return selectedTarget;
    }

    private DeploymentResultBuildItem deploy(DeploymentTargetEntry deploymentTarget,
            KubernetesClient client, Path outputDir,
            OpenShiftConfig openshiftConfig, ApplicationInfoBuildItem applicationInfo,
            List<KubernetesOptionalResourceDefinitionBuildItem> optionalResourceDefinitions) {
        String namespace = Optional.ofNullable(client.getNamespace()).orElse("default");
        log.info("Deploying to " + deploymentTarget.getName().toLowerCase() + " server: " + client.getMasterUrl()
                + " in namespace: " + namespace + ".");
        File manifest = outputDir.resolve(deploymentTarget.getName().toLowerCase() + ".yml").toFile();

        try (FileInputStream fis = new FileInputStream(manifest)) {
            KubernetesList list = Serialization.unmarshalAsList(fis);

            Optional<GenericKubernetesResource> conflictingResource = findConflictingResource(client, deploymentTarget,
                    list.getItems());
            if (conflictingResource.isPresent()) {
                String messsage = "Skipping deployment of " + deploymentTarget.getDeploymentResourceKind() + " "
                        + conflictingResource.get().getMetadata().getName() + " because a "
                        + conflictingResource.get().getKind() + " with the same name exists.";
                log.warn(messsage);
                log.warn("This may occur when switching deployment targets, or when the default deployment target is changed.");
                log.warn("Please remove conflicting resource and try again.");
                throw new IllegalStateException(messsage);
            }

            list.getItems().stream().filter(distinctByResourceKey()).forEach(i -> {
                Optional<HasMetadata> existing = Optional.ofNullable(client.resource(i).get());
                checkLabelSelectorVersions(deploymentTarget, i, existing);
            });

            list.getItems().stream().filter(distinctByResourceKey()).forEach(i -> {
                deployResource(deploymentTarget, client, i, optionalResourceDefinitions);
                log.info("Applied: " + i.getKind() + " " + i.getMetadata().getName() + ".");
            });

            printExposeInformation(client, list, openshiftConfig, applicationInfo);

            HasMetadata m = list.getItems().stream()
                    .filter(r -> deploymentTarget.getDeploymentResourceKind().matches(r))
                    .findFirst().orElseThrow(() -> new IllegalStateException(
                            "No " + deploymentTarget.getDeploymentResourceKind() + " found under: "
                                    + manifest.getAbsolutePath()));
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

    private void deployResource(DeploymentTargetEntry deploymentTarget, KubernetesClient client, HasMetadata metadata,
            List<KubernetesOptionalResourceDefinitionBuildItem> optionalResourceDefinitions) {
        var r = findResource(client, metadata);
        Optional<HasMetadata> existing = Optional.ofNullable(client.resource(metadata).get());
        if (shouldDeleteExisting(deploymentTarget, metadata)) {
            deleteResource(metadata, r);
        }

        try {
            switch (deploymentTarget.getDeployStrategy()) {
                case Create:
                    r.create();
                    break;
                case Replace:
                    r.replace();
                    break;
                case ServerSideApply:
                    r.patch(PatchContext.of(PatchType.SERVER_SIDE_APPLY));
                    break;
                default:
                    r.createOrReplace();
                    break;
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw e;
            } else if (isOptional(optionalResourceDefinitions, metadata)) {
                log.warn("Failed to apply: " + metadata.getKind() + " " + metadata.getMetadata().getName()
                        + ", possibly due to missing a CRD apiVersion: " + metadata.getApiVersion() + " and kind: "
                        + metadata.getKind() + ".");
            } else {
                throw e;
            }
        }
    }

    private Optional<GenericKubernetesResource> findConflictingResource(KubernetesClient clinet,
            DeploymentTargetEntry deploymentTarget, List<HasMetadata> generated) {
        HasMetadata deploymentResource = generated.stream()
                .filter(r -> deploymentTarget.getDeploymentResourceKind().matches(r))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No " + deploymentTarget.getDeploymentResourceKind() + " found under: " + deploymentTarget.getName()));
        String name = deploymentResource.getMetadata().getName();

        for (DeploymentResourceKind deploymentKind : DeploymentResourceKind.values()) {
            if (deploymentKind.matches(deploymentResource)) {
                continue;
            }
            try {
                GenericKubernetesResource resource = clinet
                        .genericKubernetesResources(deploymentKind.getApiVersion(), deploymentKind.getKind()).withName(name)
                        .get();
                if (resource != null) {
                    log.warn("Found conflicting resource:" + resource.getApiVersion() + "/" + resource.getKind() + ":"
                            + resource.getMetadata().getName());
                    return Optional.of(resource);
                }
            } catch (KubernetesClientException e) {
                // ignore
            }
        }
        return Optional.empty();
    }

    private void deleteResource(HasMetadata metadata, Resource<?> r) {
        r.delete();
        try {
            r.waitUntilCondition(Objects::isNull, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw e;
            }
            //This is something that should not really happen. it's not a fatal condition so let's just log.
            log.warn("Failed to wait for the deletion of: " + metadata.getApiVersion() + " " + metadata.getKind() + " "
                    + metadata.getMetadata().getName() + ". Is the resource waitable?");
        }
    }

    private Resource<?> findResource(KubernetesClient client, HasMetadata metadata) {
        if (metadata instanceof GenericKubernetesResource) {
            GenericKubernetesResource genericResource = (GenericKubernetesResource) metadata;
            ResourceDefinitionContext context = getGenericResourceContext(client, genericResource)
                    .orElseThrow(() -> new IllegalStateException("Could not retrieve API resource information for:"
                            + metadata.getApiVersion() + " " + metadata.getKind()
                            + ". Is the CRD for the resource available?"));

            return client.genericKubernetesResources(context).resource(genericResource);
        }

        return client.resource(metadata);
    }

    private void printExposeInformation(KubernetesClient client, KubernetesList list, OpenShiftConfig openshiftConfig,
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
        if (deploymentTarget.getDeployStrategy() != DeployStrategy.CreateOrUpdate) {
            return false;
        }
        return KNATIVE.equalsIgnoreCase(deploymentTarget.getName())
                || resource instanceof Service
                || (Objects.equals("v1", resource.getApiVersion()) && Objects.equals("Service", resource.getKind()))
                || resource instanceof Job
                || (Objects.equals("batch/v1", resource.getApiVersion()) && Objects.equals("Job", resource.getKind()));
    }

    private static Predicate<HasMetadata> distinctByResourceKey() {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(t.getApiVersion() + "/" + t.getKind() + ":" + t.getMetadata().getName(),
                Boolean.TRUE) == null;
    }

    private static void checkLabelSelectorVersions(DeploymentTargetEntry deploymnetTarget, HasMetadata resource,
            Optional<HasMetadata> existing) {
        if (!existing.isPresent()) {
            return;
        }

        if (resource instanceof Deployment) {
            Optional<String> version = getLabelSelectorVersion(resource);
            Optional<String> existingVersion = getLabelSelectorVersion(existing.get());
            if (version.isPresent() && existingVersion.isPresent()) {
                if (!version.get().equals(existingVersion.get())) {
                    throw new IllegalStateException(String.format(
                            "A previous Deployment with a conflicting label %s=%s was found in the label selector (current is %s=%s). As the label selector is immutable, you need to either align versions or manually delete previous deployment.",
                            VERSION_LABEL, existingVersion.get(), VERSION_LABEL, version.get()));
                }
            } else if (version.isPresent()) {
                throw new IllegalStateException(String.format(
                        "A Deployment with a conflicting label %s=%s was in the label selector was requested (previous had no such label). As the label selector is immutable, you need to either manually delete previous deployment, or remove the label (consider using quarkus.%s.add-version-to-label-selectors=false).",
                        VERSION_LABEL, version.get(), deploymnetTarget.getName().toLowerCase()));
            } else if (existingVersion.isPresent()) {
                throw new IllegalStateException(String.format(
                        "A Deployment with no label in the label selector was requested (previous includes %s=%s). As the label selector is immutable, you need to either manually delete previous deployment, or ensure the %s label is present (consider using quarkus.%s.add-version-to-label-selectors=true).",
                        VERSION_LABEL, existingVersion.get(), VERSION_LABEL, deploymnetTarget.getName().toLowerCase()));
            }
        }
    }

    private static Optional<String> getLabelSelectorVersion(HasMetadata resource) {
        AtomicReference<String> version = new AtomicReference<>();
        KubernetesList list = new KubernetesListBuilder().addToItems(resource).accept(new Visitor<LabelSelectorFluent<?>>() {
            @Override
            public void visit(LabelSelectorFluent<?> item) {
                if (item.getMatchLabels() != null) {
                    version.set(item.getMatchLabels().get(VERSION_LABEL));
                }
            }
        }).build();
        return Optional.ofNullable(version.get());
    }
}
