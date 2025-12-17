package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;
import static io.quarkus.kubernetes.deployment.KubernetesCommonHelper.printMessageAboutPortsThatCantChange;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.MANAGEMENT_PORT_NAME;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.managementPortIsEnabled;
import static io.quarkus.kubernetes.deployment.OpenShiftConfig.OpenshiftFlavor.v3;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.ImageConfiguration;
import io.dekorate.kubernetes.config.ImageConfigurationBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
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
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBindingBuildItem;
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

public class OpenshiftProcessor extends BaseKubeProcessor<AddPortToOpenshiftConfig, OpenShiftConfig> {
    private static final String DOCKERIO_REGISTRY = "docker.io";
    private static final String OPENSHIFT_V3_APP = "app";
    private OpenShiftConfig config;

    @Override
    protected int priority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    protected String deploymentTarget() {
        return OPENSHIFT;
    }

    @Override
    protected String clusterType() {
        return OPENSHIFT;
    }

    @Override
    protected OpenShiftConfig config() {
        return config;
    }

    @Override
    protected boolean enabled() {
        List<String> targets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        return targets.contains(deploymentTarget());
    }

    @Override
    protected DeploymentResourceKind deploymentResourceKind(Capabilities capabilities) {
        return config.getDeploymentResourceKind(capabilities);
    }

    @BuildStep
    public void checkOpenshift(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        super.produceDeploymentBuildItem(applicationInfo, capabilities, deploymentTargets, resourceMeta);
    }

    @BuildStep
    public void populateInternalRegistry(ContainerImageConfig containerImageConfig,
            Capabilities capabilities,
            BuildProducer<FallbackContainerImageRegistryBuildItem> containerImageRegistry,
            BuildProducer<SingleSegmentContainerImageRequestBuildItem> singleSegmentContainerImageRequest) {

        if (containerImageConfig.registry().isEmpty() && containerImageConfig.image().isEmpty()) {
            DeploymentResourceKind deploymentResourceKind = deploymentResourceKind(capabilities);
            if (deploymentResourceKind != DeploymentResourceKind.DeploymentConfig) {
                if (OpenShiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                    //Don't need fallback namespace, we use local lookup instead.
                    singleSegmentContainerImageRequest.produce(new SingleSegmentContainerImageRequestBuildItem());
                } else {
                    containerImageRegistry.produce(new FallbackContainerImageRegistryBuildItem(DOCKERIO_REGISTRY));
                }
            }
        }
    }

    @BuildStep
    public void createAnnotations(BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        super.createAnnotations(annotations);
    }

    @BuildStep
    public void createLabels(BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        super.createLabels(labels, imageLabels);
    }

    @BuildStep
    public void createNamespace(BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        super.createNamespace(namespace);
    }

    @Override
    protected AddPortToOpenshiftConfig portConfigurator(Port port) {
        return new AddPortToOpenshiftConfig(port);
    }

    @Override
    protected Optional<Port> optionalPort(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config, config.route().targetPort());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(Capabilities capabilities,
            Optional<ContainerImageInfoBuildItem> image,
            List<KubernetesPortBuildItem> ports) {
        final var config = config();
        List<ConfiguratorBuildItem> result = super.createConfigurators(ports);

        result.add(new ConfiguratorBuildItem(new ApplyOpenshiftRouteConfigurator(config.route())));

        // Handle remote debug configuration for container ports
        if (config.remoteDebug().enabled()) {
            result.add(new ConfiguratorBuildItem(new AddPortToOpenshiftConfig(config.remoteDebug().buildDebugPort())));
        }

        if (!capabilities.isPresent("io.quarkus.openshift")
                && !capabilities.isPresent(Capability.CONTAINER_IMAGE_OPENSHIFT)) {
            result.add(new ConfiguratorBuildItem(new DisableS2iConfigurator()));

            image.flatMap(ContainerImageInfoBuildItem::getRegistry)
                    .ifPresent(r -> result.add(new ConfiguratorBuildItem(new ApplyImageRegistryConfigurator(r))));

            image.map(ContainerImageInfoBuildItem::getGroup)
                    .ifPresent(g -> result.add(new ConfiguratorBuildItem(new ApplyImageGroupConfigurator(g))));

        }
        return result;
    }

    @BuildStep
    public KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(ApplicationInfoBuildItem applicationInfo,
            List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        return super.computeEffectiveServiceAccounts(applicationInfo, serviceAccountsFromExtensions, decorators);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
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
            List<KubernetesEffectiveServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets) {
        final var context = commonDecorators(applicationInfo, outputTarget, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, annotations, labels, envs, image, command,
                ports, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                clusterRoleBindings, customProjectRoot, targets);
        if (context.done()) {
            return context.decorators();
        }

        final var config = config();
        final var clusterKind = deploymentTarget();
        final var name = context.name();

        // additional image configuration
        image.ifPresent(i -> {
            String registry = i.registry
                    .or(containerImageConfig::registry)
                    .orElse(fallbackRegistry.map(FallbackContainerImageRegistryBuildItem::getRegistry)
                            .orElse(DOCKERIO_REGISTRY));
            String repositoryWithRegistry = registry + "/" + i.getRepository();
            ImageConfiguration imageConfiguration = new ImageConfigurationBuilder()
                    .withName(name)
                    .withRegistry(registry)
                    .build();

            String imageStreamWithTag = name + ":" + i.getTag();
            if (deploymentResourceKind(capabilities) == DeploymentResourceKind.DeploymentConfig
                    && !OpenShiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                context.add(new AddDockerImageStreamResourceDecorator(imageConfiguration, repositoryWithRegistry));
            }

            // remove the default trigger which has a wrong version
            context.add(new RemoveDeploymentTriggerDecorator(name));
            // re-add the trigger with the correct version
            context.add(new ChangeDeploymentTriggerDecorator(name, imageStreamWithTag));
        });

        if (config.flavor() == v3) {
            //Openshift 3.x doesn't recognize 'app.kubernetes.io/name', it uses 'app' instead.
            //The decorator will be applied even on non-openshift resources is it may affect for example: knative
            if (labels.stream().filter(l -> clusterKind.equals(l.getTarget()))
                    .noneMatch(l -> l.getKey().equals(OPENSHIFT_V3_APP))) {
                context.addToAnyTarget(new AddLabelDecorator(name, OPENSHIFT_V3_APP, name));
            }

            // The presence of optional is causing issues in OCP 3.11, so we better remove them.
            // The following 4 decorator will set the optional property to null, so that it won't make it into the file.
            //The decorators will be applied even on non-openshift resources is they may affect for example: knative
            context.addToAnyTarget(new RemoveOptionalFromSecretEnvSourceDecorator());
            context.addToAnyTarget(new RemoveOptionalFromConfigMapEnvSourceDecorator());
            context.addToAnyTarget(new RemoveOptionalFromSecretKeySelectorDecorator());
            context.addToAnyTarget(new RemoveOptionalFromConfigMapKeySelectorDecorator());
        }

        context.addToAnyTarget(new ApplyResolveNamesImagePolicyDecorator());

        DeploymentResourceKind deploymentKind = config.getDeploymentResourceKind(capabilities);
        switch (deploymentKind) {
            case Deployment:
                context.add(new RemoveDeploymentConfigResourceDecorator(name));
                context.add(new AddDeploymentResourceDecorator(name, config));
                break;
            case StatefulSet:
                context.add(new RemoveDeploymentConfigResourceDecorator(name));
                context.add(new AddStatefulSetResourceDecorator(name, config));
                break;
            case Job:
                context.add(new RemoveDeploymentConfigResourceDecorator(name));
                context.add(new AddJobResourceDecorator(name, config.job()));
                break;
            case CronJob:
                context.add(new RemoveDeploymentConfigResourceDecorator(name));
                context.add(new AddCronJobResourceDecorator(name, config.cronJob()));
                break;
        }

        if (config.route() != null) {
            for (Map.Entry<String, String> annotation : config.route().annotations().entrySet()) {
                context.add(new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), ROUTE));
            }

            for (Map.Entry<String, String> label : config.route().labels().entrySet()) {
                context.add(new AddLabelDecorator(name, label.getKey(), label.getValue(), ROUTE));
            }
        }

        if (config.replicas() != 1) {
            // This only affects DeploymentConfig
            context.add(new ApplyReplicasToDeploymentConfigDecorator(name, config.replicas()));
            // This only affects Deployment
            context.add(new io.dekorate.kubernetes.decorator.ApplyReplicasToDeploymentDecorator(name, config.replicas()));
            // This only affects StatefulSet
            context.add(new ApplyReplicasToStatefulSetDecorator(name, config.replicas()));
        }

        config.containerName().ifPresent(containerName -> {
            context.add(new ChangeContainerNameDecorator(containerName));
            context.add(new ChangeContainerNameInDeploymentTriggerDecorator(containerName));
        });

        if (labels.stream().filter(l -> clusterKind.equals(l.getTarget()))
                .noneMatch(l -> l.getKey().equals(OPENSHIFT_APP_RUNTIME))) {
            context.add(new AddLabelDecorator(name, OPENSHIFT_APP_RUNTIME, QUARKUS));
        }

        // Enalbe local lookup policy for all image streams
        context.add(new EnableImageStreamLocalLookupPolicyDecorator());

        // Handle custom s2i builder images
        baseImage.map(BaseImageInfoBuildItem::getImage).ifPresent(builderImage -> {
            String builderImageName = ImageUtil.getName(builderImage);
            if (!DEFAULT_S2I_IMAGE_NAME.equals(builderImageName)) {
                context.add(new RemoveBuilderImageResourceDecorator(DEFAULT_S2I_IMAGE_NAME));
            }

            if (containerImageConfig.builder().isEmpty()
                    || OpenShiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                context.add(new ApplyBuilderImageDecorator(name, builderImage));
                ImageReference imageRef = ImageReference.parse(builderImage);
                boolean usesInternalRegistry = imageRef.getRegistry()
                        .filter(registry -> registry.contains(OPENSHIFT_INTERNAL_REGISTRY_PROJECT))
                        .isPresent();
                if (usesInternalRegistry) {
                    // When the internal registry is specified for the image, we assume the stream already exists
                    // It's better if we refer to it directly as (as an ImageStreamTag).
                    // In this case we need to remove the ImageStream (created by dekorate).
                    String repository = imageRef.getRepository();
                    String imageStreamName = repository.substring(repository.lastIndexOf("/"));
                    context.add(new RemoveBuilderImageResourceDecorator(imageStreamName));
                } else {
                    S2iBuildConfig s2iBuildConfig = new S2iBuildConfigBuilder().withBuilderImage(builderImage).build();
                    context.add(new AddBuilderImageStreamResourceDecorator(s2iBuildConfig));
                }
            }
        });

        // Service handling
        context.add(new ApplyServiceTypeDecorator(name, config.serviceType().name()));
        if ((config.serviceType() == ServiceType.NodePort) && config.nodePort().isPresent()) {
            context.add(new AddNodePortDecorator(name, config.nodePort().getAsInt(), config.route().targetPort()));
        }

        // Probe port handling
        probes(context, ports, portName);

        // Handle init Containers and Jobs
        initTasks(context, initContainers, jobs);

        // Handle remote debug configuration
        if (config.remoteDebug().enabled()) {
            context.add(new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                    config.remoteDebug().buildJavaToolOptionsEnv()));
        }

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        if (managementPortIsEnabled()
                && (config.route() == null
                        || !config.route().expose()
                        || !config.route().targetPort().equals(MANAGEMENT_PORT_NAME))) {
            context.add(new RemovePortFromServiceDecorator(name, MANAGEMENT_PORT_NAME));
        }

        printMessageAboutPortsThatCantChange(clusterKind, ports, config);
        return context.decorators();
    }

    @BuildStep
    protected void externalizeInitTasks(
            ApplicationInfoBuildItem applicationInfo,
            ContainerImageInfoBuildItem image,
            List<InitTaskBuildItem> initTasks,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccount,
            BuildProducer<DecoratorBuildItem> decorators) {
        super.externalizeInitTasks(applicationInfo, image, initTasks, jobs, initContainers, env, roles, roleBindings,
                serviceAccount, decorators);
    }
}
