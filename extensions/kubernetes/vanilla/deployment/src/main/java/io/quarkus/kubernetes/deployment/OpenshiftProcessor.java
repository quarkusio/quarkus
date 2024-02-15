
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_S2I_IMAGE_NAME;
import static io.quarkus.kubernetes.deployment.Constants.LIVENESS_PROBE;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_APP_RUNTIME;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_INTERNAL_REGISTRY_PROJECT;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS;
import static io.quarkus.kubernetes.deployment.Constants.READINESS_PROBE;
import static io.quarkus.kubernetes.deployment.Constants.ROUTE;
import static io.quarkus.kubernetes.deployment.Constants.STARTUP_PROBE;
import static io.quarkus.kubernetes.deployment.KubernetesCommonHelper.printMessageAboutPortsThatCantChange;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.MANAGEMENT_PORT_NAME;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.managementPortIsEnabled;
import static io.quarkus.kubernetes.deployment.OpenshiftConfig.OpenshiftFlavor.v3;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.ImageConfiguration;
import io.dekorate.kubernetes.config.ImageConfigurationBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.openshift.decorator.ApplyReplicasToDeploymentConfigDecorator;
import io.dekorate.project.Project;
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
import io.quarkus.kubernetes.deployment.OpenshiftConfig.DeploymentResourceKind;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthStartupPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesInitContainerBuildItem;
import io.quarkus.kubernetes.spi.KubernetesJobBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesProbePortNameBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;

public class OpenshiftProcessor {

    private static final int OPENSHIFT_PRIORITY = DEFAULT_PRIORITY;
    private static final String DOCKERIO_REGISTRY = "docker.io";
    private static final String OPENSHIFT_V3_APP = "app";
    private static final String ANY = null;

    @BuildStep
    public void checkOpenshift(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities, OpenshiftConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        List<String> targets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        boolean openshiftEnabled = targets.contains(OPENSHIFT);

        DeploymentResourceKind deploymentResourceKind = config.getDeploymentResourceKind(capabilities);
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(OPENSHIFT, deploymentResourceKind.kind, deploymentResourceKind.apiGroup,
                        deploymentResourceKind.apiVersion, OPENSHIFT_PRIORITY, openshiftEnabled, config.deployStrategy));
        if (openshiftEnabled) {
            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(new KubernetesResourceMetadataBuildItem(OPENSHIFT, deploymentResourceKind.apiGroup,
                    deploymentResourceKind.apiVersion, deploymentResourceKind.kind, name));
        }
    }

    @BuildStep
    public void populateInternalRegistry(OpenshiftConfig openshiftConfig, ContainerImageConfig containerImageConfig,
            Capabilities capabilities,
            BuildProducer<FallbackContainerImageRegistryBuildItem> containerImageRegistry,
            BuildProducer<SingleSegmentContainerImageRequestBuildItem> singleSegmentContainerImageRequest) {

        if (!containerImageConfig.registry.isPresent() && !containerImageConfig.image.isPresent()) {
            DeploymentResourceKind deploymentResourceKind = openshiftConfig.getDeploymentResourceKind(capabilities);
            if (deploymentResourceKind != DeploymentResourceKind.DeploymentConfig) {
                if (openshiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
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
        config.getAnnotations().forEach((k, v) -> {
            annotations.produce(new KubernetesAnnotationBuildItem(k, v, OPENSHIFT));
        });
    }

    @BuildStep
    public void createLabels(OpenshiftConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config.getLabels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, OPENSHIFT));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
        labels.produce(new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", OPENSHIFT));
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
            List<KubernetesDeploymentTargetBuildItem> targets) {

        List<DecoratorBuildItem> result = new ArrayList<>();
        if (!targets.stream().filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .anyMatch(t -> OPENSHIFT.equals(t.getName()))) {
            return result;
        }

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = KubernetesCommonHelper.getPort(ports, config, config.route.targetPort);
        result.addAll(KubernetesCommonHelper.createDecorators(project, OPENSHIFT, name, config,
                metricsConfiguration, kubernetesClientConfiguration,
                annotations, labels, image, command,
                port, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings));

        if (config.flavor == v3) {
            //Openshift 3.x doesn't recognize 'app.kubernetes.io/name', it uses 'app' instead.
            //The decorator will be applied even on non-openshift resources is it may affect for example: knative
            if (labels.stream().filter(l -> OPENSHIFT.equals(l.getTarget()))
                    .noneMatch(l -> l.getKey().equals(OPENSHIFT_V3_APP))) {
                result.add(new DecoratorBuildItem(new AddLabelDecorator(name, OPENSHIFT_V3_APP, name)));
            }

            // The presence of optional is causing issues in OCP 3.11, so we better remove them.
            // The following 4 decorator will set the optional property to null, so that it won't make it into the file.
            //The decorators will be applied even on non-openshift resources is they may affect for example: knative
            result.add(new DecoratorBuildItem(new RemoveOptionalFromSecretEnvSourceDecorator()));
            result.add(new DecoratorBuildItem(new RemoveOptionalFromConfigMapEnvSourceDecorator()));
            result.add(new DecoratorBuildItem(new RemoveOptionalFromSecretKeySelectorDecorator()));
            result.add(new DecoratorBuildItem(new RemoveOptionalFromConfigMapKeySelectorDecorator()));
        }

        result.add(new DecoratorBuildItem(new ApplyResolveNamesImagePolicyDecorator()));

        DeploymentResourceKind deploymentKind = config.getDeploymentResourceKind(capabilities);
        switch (deploymentKind) {
            case Deployment:
                result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveDeploymentConfigResourceDecorator(name)));
                result.add(new DecoratorBuildItem(OPENSHIFT, new AddDeploymentResourceDecorator(name, config)));
                break;
            case StatefulSet:
                result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveDeploymentConfigResourceDecorator(name)));
                result.add(new DecoratorBuildItem(OPENSHIFT, new AddStatefulSetResourceDecorator(name, config)));
                break;
            case Job:
                result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveDeploymentConfigResourceDecorator(name)));
                result.add(new DecoratorBuildItem(OPENSHIFT, new AddJobResourceDecorator(name, config.job)));
                break;
            case CronJob:
                result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveDeploymentConfigResourceDecorator(name)));
                result.add(new DecoratorBuildItem(OPENSHIFT, new AddCronJobResourceDecorator(name, config.cronJob)));
                break;
        }

        if (config.route != null) {
            for (Map.Entry<String, String> annotation : config.route.annotations.entrySet()) {
                result.add(new DecoratorBuildItem(OPENSHIFT,
                        new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), ROUTE)));
            }
        }

        if (config.getReplicas() != 1) {
            // This only affects DeploymentConfig
            result.add(new DecoratorBuildItem(OPENSHIFT,
                    new ApplyReplicasToDeploymentConfigDecorator(name, config.getReplicas())));
            // This only affects Deployment
            result.add(new DecoratorBuildItem(OPENSHIFT,
                    new io.dekorate.kubernetes.decorator.ApplyReplicasToDeploymentDecorator(name, config.getReplicas())));
            // This only affects StatefulSet
            result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyReplicasToStatefulSetDecorator(name, config.getReplicas())));
        }

        config.getContainerName().ifPresent(containerName -> {
            result.add(new DecoratorBuildItem(OPENSHIFT, new ChangeContainerNameDecorator(containerName)));
            result.add(new DecoratorBuildItem(OPENSHIFT, new ChangeContainerNameInDeploymentTriggerDecorator(containerName)));
        });

        result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyImagePullPolicyDecorator(name, config.getImagePullPolicy())));

        if (labels.stream().filter(l -> OPENSHIFT.equals(l.getTarget()))
                .noneMatch(l -> l.getKey().equals(OPENSHIFT_APP_RUNTIME))) {
            result.add(new DecoratorBuildItem(OPENSHIFT, new AddLabelDecorator(name, OPENSHIFT_APP_RUNTIME, QUARKUS)));
        }

        Stream.concat(config.convertToBuildItems().stream(),
                envs.stream().filter(e -> e.getTarget() == null || OPENSHIFT.equals(e.getTarget()))).forEach(e -> {
                    result.add(new DecoratorBuildItem(OPENSHIFT,
                            new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                                    new EnvBuilder().withName(EnvConverter.convertName(e.getName())).withValue(e.getValue())
                                            .withSecret(e.getSecret()).withConfigmap(e.getConfigMap()).withField(e.getField())
                                            .build())));
                });

        // Enalbe local lookup policy for all image streams
        result.add(new DecoratorBuildItem(OPENSHIFT, new EnableImageStreamLocalLookupPolicyDecorator()));

        // Handle custom s2i builder images
        baseImage.map(BaseImageInfoBuildItem::getImage).ifPresent(builderImage -> {
            String builderImageName = ImageUtil.getName(builderImage);
            if (!DEFAULT_S2I_IMAGE_NAME.equals(builderImageName)) {
                result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveBuilderImageResourceDecorator(DEFAULT_S2I_IMAGE_NAME)));
            }

            if (containerImageConfig.builder.isEmpty() || config.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyBuilderImageDecorator(name, builderImage)));
                ImageReference imageRef = ImageReference.parse(builderImage);
                boolean usesInternalRegistry = imageRef.getRegistry()
                        .filter(registry -> registry.contains(OPENSHIFT_INTERNAL_REGISTRY_PROJECT)).isPresent();
                if (usesInternalRegistry) {
                    // When the internal registry is specified for the image, we assume the stream already exists
                    // It's better if we refer to it directly as (as an ImageStreamTag).
                    // In this case we need to remove the ImageStream (created by dekorate).
                    String repository = imageRef.getRepository();
                    String imageStreamName = repository.substring(repository.lastIndexOf("/"));
                    result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveBuilderImageResourceDecorator(imageStreamName)));
                } else {
                    S2iBuildConfig s2iBuildConfig = new S2iBuildConfigBuilder().withBuilderImage(builderImage).build();
                    result.add(new DecoratorBuildItem(OPENSHIFT, new AddBuilderImageStreamResourceDecorator(s2iBuildConfig)));
                }
            }
        });

        // Service handling
        result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));
        if ((config.getServiceType() == ServiceType.NodePort) && config.nodePort.isPresent()) {
            result.add(new DecoratorBuildItem(OPENSHIFT,
                    new AddNodePortDecorator(name, config.nodePort.getAsInt(), config.route.targetPort)));
        }

        // Probe port handling
        result.add(KubernetesCommonHelper.createProbeHttpPortDecorator(name, OPENSHIFT, LIVENESS_PROBE, config.livenessProbe,
                portName,
                ports,
                config.ports));
        result.add(KubernetesCommonHelper.createProbeHttpPortDecorator(name, OPENSHIFT, READINESS_PROBE, config.readinessProbe,
                portName,
                ports,
                config.ports));
        result.add(KubernetesCommonHelper.createProbeHttpPortDecorator(name, OPENSHIFT, STARTUP_PROBE, config.startupProbe,
                portName,
                ports,
                config.ports));

        image.ifPresent(i -> {
            String registry = i.registry
                    .or(() -> containerImageConfig.registry)
                    .orElse(fallbackRegistry.map(f -> f.getRegistry()).orElse(DOCKERIO_REGISTRY));
            String repositoryWithRegistry = registry + "/" + i.getRepository();
            ImageConfiguration imageConfiguration = new ImageConfigurationBuilder()
                    .withName(name)
                    .withRegistry(registry)
                    .build();

            String imageStreamWithTag = name + ":" + i.getTag();
            if (deploymentKind == DeploymentResourceKind.DeploymentConfig
                    && !OpenshiftConfig.isOpenshiftBuildEnabled(containerImageConfig, capabilities)) {
                result.add(new DecoratorBuildItem(OPENSHIFT,
                        new AddDockerImageStreamResourceDecorator(imageConfiguration, repositoryWithRegistry)));
            }
            result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyContainerImageDecorator(name, i.getImage())));
            // remove the default trigger which has a wrong version
            result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveDeploymentTriggerDecorator(name)));
            // re-add the trigger with the correct version
            result.add(new DecoratorBuildItem(OPENSHIFT, new ChangeDeploymentTriggerDecorator(name, imageStreamWithTag)));
        });

        // Handle remote debug configuration
        if (config.remoteDebug.enabled) {
            result.add(new DecoratorBuildItem(OPENSHIFT, new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                    config.remoteDebug.buildJavaToolOptionsEnv())));
        }

        // Handle init Containers and Jobs
        result.addAll(KubernetesCommonHelper.createInitContainerDecorators(OPENSHIFT, name, initContainers, result));
        result.addAll(KubernetesCommonHelper.createInitJobDecorators(OPENSHIFT, name, jobs, result));

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        if (managementPortIsEnabled()
                && (config.route == null
                        || !config.route.expose
                        || !config.route.targetPort.equals(MANAGEMENT_PORT_NAME))) {
            result.add(new DecoratorBuildItem(OPENSHIFT, new RemovePortFromServiceDecorator(name, MANAGEMENT_PORT_NAME)));
        }

        printMessageAboutPortsThatCantChange(OPENSHIFT, ports, config);
        return result;
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
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        if (config.externalizeInit) {
            InitTaskProcessor.process(OPENSHIFT, name, image, initTasks, config.initTaskDefaults, config.initTasks,
                    jobs, initContainers, env, roles, roleBindings, serviceAccount, decorators);
        }
    }
}
