package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.MANAGEMENT_PORT_NAME;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.managementPortIsEnabled;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyReplicasToDeploymentDecorator;
import io.dekorate.kubernetes.decorator.ApplyReplicasToStatefulSetDecorator;
import io.dekorate.project.Project;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
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
import io.quarkus.kubernetes.spi.KubernetesNamespaceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesProbePortNameBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;

public abstract class BaseProcessor<C extends PlatformConfiguration> {
    protected final String flavor;
    protected final int priority;
    protected final String deploymentTarget;

    public BaseProcessor(String flavor, int priority) {
        this(flavor, priority, flavor);
    }

    public BaseProcessor(String flavor, int priority, String deploymentTarget) {
        this.flavor = flavor;
        this.priority = priority;
        this.deploymentTarget = deploymentTarget;
    }

    protected void doCheckEnabled(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities,
            C config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        List<String> targets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        boolean flavorIsEnabled = targets.contains(flavor);

        DeploymentResourceKind deploymentResourceKind = config.getDeploymentResourceKind(capabilities);
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(flavor, deploymentResourceKind.getKind(),
                        deploymentResourceKind.getGroup(),
                        deploymentResourceKind.getVersion(), priority, flavorIsEnabled, config.getDeployStrategy()));
        if (flavorIsEnabled) {
            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(new KubernetesResourceMetadataBuildItem(flavor, deploymentResourceKind.getGroup(),
                    deploymentResourceKind.getVersion(), deploymentResourceKind.getKind(), name));
        }
    }

    protected void doCreateAnnotations(C config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.getAnnotations().forEach((k, v) -> annotations.produce(new KubernetesAnnotationBuildItem(k, v, flavor)));
    }

    protected void doCreateLabels(C config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config.getLabels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, flavor));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
    }

    protected void doCreateNamespace(C config, BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        config.getNamespace().ifPresent(n -> namespace.produce(new KubernetesNamespaceBuildItem(flavor, n)));
    }

    protected abstract Optional<Port> createPort(C config, List<KubernetesPortBuildItem> ports);

    protected KubernetesCommonHelper.ManifestGenerationInfo doCreateDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            C config,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            List<KubernetesNamespaceBuildItem> namespaces,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            List<KubernetesEnvBuildItem> envs,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            List<KubernetesPortBuildItem> ports,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            Optional<KubernetesHealthStartupPathBuildItem> startupProbePath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesClusterRoleBuildItem> clusterRoles,
            List<KubernetesServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets) {
        String name = ResourceNameUtil.getResourceName(config, applicationInfo);

        if (!targets.isEmpty() && targets.stream()
                .filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .noneMatch(t -> flavor.equals(t.getName()))) {
            return KubernetesCommonHelper.ManifestGenerationInfo.empty(name);
        }

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = createPort(config, ports);
        Optional<KubernetesNamespaceBuildItem> namespace = namespaces.stream()
                .filter(n -> flavor.equals(n.getTarget()))
                .findFirst();
        final var manifestInfo = KubernetesCommonHelper.createDecorators(project, flavor, name, namespace, config,
                metricsConfiguration, kubernetesClientConfiguration,
                annotations, labels, image, command,
                port, livenessPath, readinessPath, startupProbePath, roles, clusterRoles, serviceAccounts, roleBindings);

        // deal with environment variables
        Stream.concat(config.convertToBuildItems().stream(), envs.stream().filter(e -> e.targetsFlavor(deploymentTarget)))
                .forEach(e -> manifestInfo.add(createEnvVarDecorator(e, name)));

        containerNameDecorators(config, manifestInfo);

        return manifestInfo;
    }

    protected void replicasDecorators(C config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo) {
        final var replicas = config.getReplicas();
        // todo: shouldn't this be > 1 instead of != 1?
        if (replicas != 1) {
            final var name = manifestInfo.getDefaultName();
            // This only affects Deployment
            manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyReplicasToDeploymentDecorator(name, replicas)));
            // This only affects StatefulSet
            manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyReplicasToStatefulSetDecorator(name, replicas)));
        }
    }

    protected void containerNameDecorators(C config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo) {
        config.getContainerName().ifPresent(containerName -> manifestInfo
                .add(new DecoratorBuildItem(flavor, new ChangeContainerNameDecorator(containerName))));
    }

    protected void containerImageDecorators(C config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo,
            Optional<ContainerImageInfoBuildItem> image) {
        final var defaultName = manifestInfo.getDefaultName();
        image.ifPresent(i -> manifestInfo
                .add(new DecoratorBuildItem(flavor, new ApplyContainerImageDecorator(defaultName, i.getImage()))));
        manifestInfo.add(
                new DecoratorBuildItem(flavor, new ApplyImagePullPolicyDecorator(defaultName, config.getImagePullPolicy())));
    }

    protected void probePortDecorators(C config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo,
            Optional<KubernetesProbePortNameBuildItem> portName,
            List<KubernetesPortBuildItem> ports) {
        final var name = manifestInfo.getDefaultName();
        manifestInfo.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, flavor, LIVENESS_PROBE, config.getLivenessProbe(),
                        portName,
                        ports,
                        config.getPorts()));
        manifestInfo.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, flavor, READINESS_PROBE, config.getReadinessProbe(),
                        portName,
                        ports,
                        config.getPorts()));
        manifestInfo
                .add(KubernetesCommonHelper.createProbeHttpPortDecorator(name, flavor, STARTUP_PROBE, config.getStartupProbe(),
                        portName,
                        ports,
                        config.getPorts()));
    }

    protected void remoteDebugDecorators(C config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo) {
        final var debugConfig = config.getDebugConfig();
        if (debugConfig.enabled) {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, manifestInfo.getDefaultName(),
                            debugConfig.buildJavaToolOptionsEnv())));
        }
    }

    protected void initContainersAndJobsDecorators(C config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo,
            List<KubernetesInitContainerBuildItem> initContainers,
            List<KubernetesJobBuildItem> jobs) {
        final var name = manifestInfo.getDefaultName();
        final var decorators = manifestInfo.getDecorators();
        manifestInfo.addAll(KubernetesCommonHelper.createInitContainerDecorators(flavor, name, initContainers, decorators));
        manifestInfo.addAll(KubernetesCommonHelper.createInitJobDecorators(flavor, name, jobs, decorators));
    }

    protected void managementPortDecorators(C config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo) {
        if (managementPortIsEnabled() && !config.needsManagementPort()) {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new RemovePortFromServiceDecorator(manifestInfo.getDefaultName(), MANAGEMENT_PORT_NAME)));
        }
    }

    private DecoratorBuildItem createEnvVarDecorator(KubernetesEnvBuildItem e, String name) {
        return new DecoratorBuildItem(flavor,
                new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                        .withName(EnvConverter.convertName(e.getName()))
                        .withValue(e.getValue())
                        .withSecret(e.getSecret())
                        .withConfigmap(e.getConfigMap())
                        .withField(e.getField())
                        .withPrefix(e.getPrefix())
                        .build()));
    }

    protected void doExternalizeInitTasks(
            ApplicationInfoBuildItem applicationInfo,
            C config,
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
        if (config.isExternalizeInit()) {
            InitTaskProcessor.process(flavor, name, image, initTasks, config.getInitTaskDefaults(), config.getInitTasks(),
                    jobs, initContainers, env, roles, roleBindings, serviceAccount, decorators);
        }
    }
}
