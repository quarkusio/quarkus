
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_S2I_IMAGE_NAME;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_APP_RUNTIME;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_INTERNAL_REGISTRY_PROJECT;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS;
import static io.quarkus.kubernetes.deployment.Constants.ROUTE;
import static io.quarkus.kubernetes.deployment.KubernetesCommonHelper.printMessageAboutPortsThatCantChange;
import static io.quarkus.kubernetes.deployment.OpenshiftConfig.OpenshiftFlavor.v3;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.ImageConfiguration;
import io.dekorate.kubernetes.config.ImageConfigurationBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.openshift.decorator.ApplyReplicasToDeploymentConfigDecorator;
import io.dekorate.s2i.config.S2iBuildConfig;
import io.dekorate.s2i.config.S2iBuildConfigBuilder;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.dekorate.s2i.decorator.AddDockerImageStreamResourceDecorator;
import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.container.spi.FallbackContainerImageRegistryBuildItem;
import io.quarkus.container.spi.ImageReference;
import io.quarkus.container.spi.SingleSegmentContainerImageRequestBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEffectiveServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthStartupPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesInitContainerBuildItem;
import io.quarkus.kubernetes.spi.KubernetesJobBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesNamespaceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesProbePortNameBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class OpenshiftProcessor extends BaseProcessor<OpenshiftConfig> {

    private static final int OPENSHIFT_PRIORITY = DEFAULT_PRIORITY;
    private static final String DOCKERIO_REGISTRY = "docker.io";
    private static final String OPENSHIFT_V3_APP = "app";

    public OpenshiftProcessor() {
        super(OPENSHIFT, OPENSHIFT_PRIORITY);
    }

    @BuildStep
    public void checkOpenshift(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities, OpenshiftConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        doCheckEnabled(applicationInfo, capabilities, config, deploymentTargets, resourceMeta);
    }

    @BuildStep
    public void populateInternalRegistry(OpenshiftConfig openshiftConfig, ContainerImageConfig containerImageConfig,
            Capabilities capabilities,
            BuildProducer<FallbackContainerImageRegistryBuildItem> containerImageRegistry,
            BuildProducer<SingleSegmentContainerImageRequestBuildItem> singleSegmentContainerImageRequest) {

        if (containerImageConfig.registry.isEmpty() && containerImageConfig.image.isEmpty()) {
            DeploymentResourceKind deploymentResourceKind = openshiftConfig.getDeploymentResourceKind(capabilities);
            if (deploymentResourceKind != DeploymentResourceKind.DeploymentConfig) {
                if (OpenshiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                    //Don't need fallback namespace, we use local lookup instead.
                    singleSegmentContainerImageRequest.produce(new SingleSegmentContainerImageRequestBuildItem());
                } else {
                    containerImageRegistry.produce(new FallbackContainerImageRegistryBuildItem(DOCKERIO_REGISTRY));
                }
            }
        }
    }

    @BuildStep
    public void createAnnotations(OpenshiftConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        doCreateAnnotations(config, annotations);
    }

    @BuildStep
    public void createLabels(OpenshiftConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        doCreateLabels(config, labels, imageLabels);
        // todo: should that label be added to all flavors?
        labels.produce(new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", flavor));
    }

    @BuildStep
    public void createNamespace(OpenshiftConfig config, BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        doCreateNamespace(config, namespace);
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(ApplicationInfoBuildItem applicationInfo,
            OpenshiftConfig config, Capabilities capabilities, Optional<ContainerImageInfoBuildItem> image,
            List<KubernetesPortBuildItem> ports) {

        List<ConfiguratorBuildItem> result = new ArrayList<>();

        KubernetesCommonHelper.combinePorts(ports, config).values().forEach(value -> {
            result.add(new ConfiguratorBuildItem(new AddPortToOpenshiftConfig(value)));
        });

        result.add(new ConfiguratorBuildItem(new ApplyOpenshiftRouteConfigurator(config.route)));

        // Handle remote debug configuration for container ports
        if (config.remoteDebug.enabled) {
            result.add(new ConfiguratorBuildItem(new AddPortToOpenshiftConfig(config.remoteDebug.buildDebugPort())));
        }

        if (!capabilities.isPresent("io.quarkus.openshift")
                && !capabilities.isPresent(Capability.CONTAINER_IMAGE_OPENSHIFT)) {
            result.add(new ConfiguratorBuildItem(new DisableS2iConfigurator()));

            image.flatMap(ContainerImageInfoBuildItem::getRegistry).ifPresent(r -> {
                result.add(new ConfiguratorBuildItem(new ApplyImageRegistryConfigurator(r)));
            });

            image.map(ContainerImageInfoBuildItem::getGroup).ifPresent(g -> {
                result.add(new ConfiguratorBuildItem(new ApplyImageGroupConfigurator(g)));
            });

        }
        return result;
    }

    @Override
    protected Optional<Port> createPort(OpenshiftConfig config, List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config, config.route.targetPort);
    }

    @Override
    protected void containerNameDecorators(OpenshiftConfig config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo) {
        config.getContainerName().ifPresent(containerName -> {
            manifestInfo.add(new DecoratorBuildItem(flavor, new ChangeContainerNameDecorator(containerName)));
            manifestInfo
                    .add(new DecoratorBuildItem(flavor, new ChangeContainerNameInDeploymentTriggerDecorator(containerName)));
        });
    }

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            OpenshiftConfig config,
            ContainerImageConfig containerImageConfig,
            Optional<FallbackContainerImageRegistryBuildItem> fallbackRegistry,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            Capabilities capabilities,
            List<KubernetesInitContainerBuildItem> initContainers,
            List<KubernetesJobBuildItem> jobs,
            List<KubernetesNamespaceBuildItem> namespaces,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            List<KubernetesEnvBuildItem> envs,
            Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            Optional<KubernetesProbePortNameBuildItem> portName,
            List<KubernetesPortBuildItem> ports,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesClusterRoleBuildItem> clusterRoles,
            List<KubernetesServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets,
            BuildProducer<KubernetesEffectiveServiceAccountBuildItem> serviceAccountProducer) {

        final var manifestInfo = doCreateDecorators(applicationInfo, outputTarget, config, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, annotations, labels, envs, image, command, ports, livenessPath,
                readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings, customProjectRoot, targets);

        if (manifestInfo.skipFurtherProcessing()) {
            return manifestInfo.getDecoratorsAndProduceServiceAccountBuildItem(serviceAccountProducer);
        }

        // container image
        DeploymentResourceKind deploymentKind = config.getDeploymentResourceKind(capabilities);
        final var name = manifestInfo.getDefaultName();
        manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyImagePullPolicyDecorator(name, config.getImagePullPolicy())));
        image.ifPresent(i -> {
            String registry = i.registry
                    .or(() -> containerImageConfig.registry)
                    .orElse(fallbackRegistry.map(FallbackContainerImageRegistryBuildItem::getRegistry)
                            .orElse(DOCKERIO_REGISTRY));
            String repositoryWithRegistry = registry + "/" + i.getRepository();
            ImageConfiguration imageConfiguration = new ImageConfigurationBuilder()
                    .withName(name)
                    .withRegistry(registry)
                    .build();

            String imageStreamWithTag = name + ":" + i.getTag();
            if (deploymentKind == DeploymentResourceKind.DeploymentConfig
                    && !OpenshiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                manifestInfo.add(new DecoratorBuildItem(flavor,
                        new AddDockerImageStreamResourceDecorator(imageConfiguration, repositoryWithRegistry)));
            }
            manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyContainerImageDecorator(name, i.getImage())));
            // remove the default trigger which has a wrong version
            manifestInfo.add(new DecoratorBuildItem(flavor, new RemoveDeploymentTriggerDecorator(name)));
            // re-add the trigger with the correct version
            manifestInfo.add(new DecoratorBuildItem(flavor, new ChangeDeploymentTriggerDecorator(name, imageStreamWithTag)));
        });

        if (config.flavor == v3) {
            //Openshift 3.x doesn't recognize 'app.kubernetes.io/name', it uses 'app' instead.
            //The decorator will be applied even on non-openshift resources is it may affect for example: knative
            if (labels.stream().filter(l -> flavor.equals(l.getTarget()))
                    .noneMatch(l -> l.getKey().equals(OPENSHIFT_V3_APP))) {
                manifestInfo.add(new DecoratorBuildItem(new AddLabelDecorator(name, OPENSHIFT_V3_APP, name)));
            }

            // The presence of optional is causing issues in OCP 3.11, so we better remove them.
            // The following 4 decorator will set the optional property to null, so that it won't make it into the file.
            //The decorators will be applied even on non-openshift resources is they may affect for example: knative
            manifestInfo.add(new DecoratorBuildItem(new RemoveOptionalFromSecretEnvSourceDecorator()));
            manifestInfo.add(new DecoratorBuildItem(new RemoveOptionalFromConfigMapEnvSourceDecorator()));
            manifestInfo.add(new DecoratorBuildItem(new RemoveOptionalFromSecretKeySelectorDecorator()));
            manifestInfo.add(new DecoratorBuildItem(new RemoveOptionalFromConfigMapKeySelectorDecorator()));
        }

        manifestInfo.add(new DecoratorBuildItem(new ApplyResolveNamesImagePolicyDecorator()));

        switch (deploymentKind) {
            case Deployment:
                manifestInfo.add(new DecoratorBuildItem(flavor, new RemoveDeploymentConfigResourceDecorator(name)));
                manifestInfo.add(new DecoratorBuildItem(flavor, new AddDeploymentResourceDecorator(name, config)));
                break;
            case StatefulSet:
                manifestInfo.add(new DecoratorBuildItem(flavor, new RemoveDeploymentConfigResourceDecorator(name)));
                manifestInfo.add(new DecoratorBuildItem(flavor, new AddStatefulSetResourceDecorator(name, config)));
                break;
            case Job:
                manifestInfo.add(new DecoratorBuildItem(flavor, new RemoveDeploymentConfigResourceDecorator(name)));
                manifestInfo.add(new DecoratorBuildItem(flavor, new AddJobResourceDecorator(name, config.job)));
                break;
            case CronJob:
                manifestInfo.add(new DecoratorBuildItem(flavor, new RemoveDeploymentConfigResourceDecorator(name)));
                manifestInfo.add(new DecoratorBuildItem(flavor, new AddCronJobResourceDecorator(name, config.cronJob)));
                break;
        }

        if (config.route != null) {
            for (Map.Entry<String, String> annotation : config.route.annotations.entrySet()) {
                manifestInfo.add(new DecoratorBuildItem(flavor,
                        new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), ROUTE)));
            }

            for (Map.Entry<String, String> label : config.route.labels.entrySet()) {
                manifestInfo.add(new DecoratorBuildItem(flavor,
                        new AddLabelDecorator(name, label.getKey(), label.getValue(), ROUTE)));
            }
        }

        if (config.getReplicas() != 1) {
            super.replicasDecorators(config, manifestInfo);

            // This only affects DeploymentConfig
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new ApplyReplicasToDeploymentConfigDecorator(name, config.getReplicas())));
        }

        if (labels.stream().filter(l -> flavor.equals(l.getTarget()))
                .noneMatch(l -> l.getKey().equals(OPENSHIFT_APP_RUNTIME))) {
            manifestInfo.add(new DecoratorBuildItem(flavor, new AddLabelDecorator(name, OPENSHIFT_APP_RUNTIME, QUARKUS)));
        }

        // Enable local lookup policy for all image streams
        manifestInfo.add(new DecoratorBuildItem(flavor, new EnableImageStreamLocalLookupPolicyDecorator()));

        // Handle custom s2i builder images
        baseImage.map(BaseImageInfoBuildItem::getImage).ifPresent(builderImage -> {
            String builderImageName = ImageUtil.getName(builderImage);
            if (!DEFAULT_S2I_IMAGE_NAME.equals(builderImageName)) {
                manifestInfo
                        .add(new DecoratorBuildItem(flavor, new RemoveBuilderImageResourceDecorator(DEFAULT_S2I_IMAGE_NAME)));
            }

            if (containerImageConfig.builder.isEmpty()
                    || OpenshiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyBuilderImageDecorator(name, builderImage)));
                ImageReference imageRef = ImageReference.parse(builderImage);
                boolean usesInternalRegistry = imageRef.getRegistry()
                        .filter(registry -> registry.contains(OPENSHIFT_INTERNAL_REGISTRY_PROJECT)).isPresent();
                if (usesInternalRegistry) {
                    // When the internal registry is specified for the image, we assume the stream already exists
                    // It's better if we refer to it directly as (as an ImageStreamTag).
                    // In this case we need to remove the ImageStream (created by dekorate).
                    String repository = imageRef.getRepository();
                    String imageStreamName = repository.substring(repository.lastIndexOf("/"));
                    manifestInfo.add(new DecoratorBuildItem(flavor, new RemoveBuilderImageResourceDecorator(imageStreamName)));
                } else {
                    S2iBuildConfig s2iBuildConfig = new S2iBuildConfigBuilder().withBuilderImage(builderImage).build();
                    manifestInfo
                            .add(new DecoratorBuildItem(flavor, new AddBuilderImageStreamResourceDecorator(s2iBuildConfig)));
                }
            }
        });

        // Service handling
        manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));
        if ((config.getServiceType() == ServiceType.NodePort) && config.nodePort.isPresent()) {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new AddNodePortDecorator(name, config.nodePort.getAsInt(), config.route.targetPort)));
        }

        // Probe port handling
        probePortDecorators(config, manifestInfo, portName, ports);

        // Handle remote debug configuration
        remoteDebugDecorators(config, manifestInfo);

        // Handle init Containers and Jobs
        initContainersAndJobsDecorators(config, manifestInfo, initContainers, jobs);

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        managementPortDecorators(config, manifestInfo);

        printMessageAboutPortsThatCantChange(flavor, ports, config);
        return manifestInfo.getDecoratorsAndProduceServiceAccountBuildItem(serviceAccountProducer);
    }

    @BuildStep
    void externalizeInitTasks(
            ApplicationInfoBuildItem applicationInfo,
            OpenshiftConfig config,
            ContainerImageInfoBuildItem image,
            List<InitTaskBuildItem> initTasks,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccount,
            BuildProducer<DecoratorBuildItem> decorators) {
        doExternalizeInitTasks(applicationInfo, config, image, initTasks, jobs, initContainers, env, roles, roleBindings,
                serviceAccount, decorators);
    }
}
