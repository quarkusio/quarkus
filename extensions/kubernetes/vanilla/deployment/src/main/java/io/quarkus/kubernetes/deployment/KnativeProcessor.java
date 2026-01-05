package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.dekorate.knative.decorator.AddAwsElasticBlockStoreVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddAzureDiskVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddAzureFileVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddConfigMapVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddEmptyDirVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddHostAliasesToRevisionDecorator;
import io.dekorate.knative.decorator.AddPvcVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddSecretVolumeToRevisionDecorator;
import io.dekorate.knative.decorator.AddSidecarToRevisionDecorator;
import io.dekorate.knative.decorator.ApplyGlobalAutoscalingClassDecorator;
import io.dekorate.knative.decorator.ApplyGlobalContainerConcurrencyDecorator;
import io.dekorate.knative.decorator.ApplyGlobalRequestsPerSecondTargetDecorator;
import io.dekorate.knative.decorator.ApplyGlobalTargetUtilizationDecorator;
import io.dekorate.knative.decorator.ApplyLocalAutoscalingClassDecorator;
import io.dekorate.knative.decorator.ApplyLocalAutoscalingMetricDecorator;
import io.dekorate.knative.decorator.ApplyLocalAutoscalingTargetDecorator;
import io.dekorate.knative.decorator.ApplyLocalContainerConcurrencyDecorator;
import io.dekorate.knative.decorator.ApplyLocalTargetUtilizationPercentageDecorator;
import io.dekorate.knative.decorator.ApplyMaxScaleDecorator;
import io.dekorate.knative.decorator.ApplyMinScaleDecorator;
import io.dekorate.knative.decorator.ApplyRevisionNameDecorator;
import io.dekorate.knative.decorator.ApplyServiceAccountToRevisionSpecDecorator;
import io.dekorate.knative.decorator.ApplyTrafficDecorator;
import io.dekorate.kubernetes.config.ConfigMapVolumeBuilder;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.MountBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.SecretVolumeBuilder;
import io.dekorate.kubernetes.decorator.AddConfigMapDataDecorator;
import io.dekorate.kubernetes.decorator.AddConfigMapResourceProvidingDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddImagePullSecretToServiceAccountDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.AddMountDecorator;
import io.dekorate.kubernetes.decorator.AddServiceAccountResourceDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
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
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesNamespaceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;

public class KnativeProcessor extends BaseKubeProcessor<AddPortToKnativeConfig, KnativeConfig> {
    private static final String LATEST_REVISION = "latest";
    private static final String KNATIVE_CONFIG_AUTOSCALER = "config-autoscaler";
    private static final String KNATIVE_CONFIG_DEFAULTS = "config-defaults";
    private static final String KNATIVE_SERVING = "knative-serving";
    private static final String KNATIVE_DEV_VISIBILITY = "networking.knative.dev/visibility";
    private KnativeConfig config;

    @Override
    protected int priority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    protected String deploymentTarget() {
        return KNATIVE;
    }

    @Override
    protected String clusterType() {
        return KNATIVE;
    }

    @Override
    protected KnativeConfig config() {
        return config;
    }

    @Override
    protected boolean enabled() {
        final var targets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        return targets.contains(deploymentTarget());
    }

    @Override
    protected DeploymentResourceKind deploymentResourceKind(Capabilities capabilities) {
        return DeploymentResourceKind.KnativeService;
    }

    @BuildStep
    public void checkKnative(ApplicationInfoBuildItem applicationInfo,
            Capabilities capabilities,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        super.produceDeploymentBuildItem(applicationInfo, capabilities, deploymentTargets, resourceMeta);
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
    protected AddPortToKnativeConfig portConfigurator(Port port) {
        return new AddPortToKnativeConfig(port);
    }

    @Override
    protected Optional<Port> optionalPort(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config, "http");
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(List<KubernetesPortBuildItem> ports) {
        return asStream(ports)
                // At the moment, Knative only supports single port binding: https://github.com/knative/serving/issues/8471
                .filter(p -> p.getName().equals("http"))
                .findFirst()
                .map(value -> List.of(new ConfiguratorBuildItem(portConfigurator(value))))
                .orElse(List.of());
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
        final var name = context.name();

        config.containerName()
                .ifPresent(containerName -> context.add(new ChangeContainerNameDecorator(containerName)));
        if (config.clusterLocal()) {
            if (labels.stream().filter(l -> deploymentTarget().equals(l.getTarget()))
                    .noneMatch(l -> l.getKey().equals(KNATIVE_DEV_VISIBILITY))) {
                context.add(new AddLabelDecorator(name, KNATIVE_DEV_VISIBILITY, "cluster-local"));
            }
        }

        config.minScale().ifPresent(min -> context.add(new ApplyMinScaleDecorator(name, min)));
        config.maxScale().ifPresent(max -> context.add(new ApplyMaxScaleDecorator(name, max)));
        config.revisionAutoScaling().autoScalerClass()
                .map(AutoScalerClassConverter::convert)
                .ifPresent(a -> context.add(new ApplyLocalAutoscalingClassDecorator(name, a)));
        config.revisionAutoScaling().metric().map(AutoScalingMetricConverter::convert)
                .ifPresent(m -> context.add(new ApplyLocalAutoscalingMetricDecorator(name, m)));
        config.revisionAutoScaling().containerConcurrency().ifPresent(
                c -> context.add(new ApplyLocalContainerConcurrencyDecorator(name, c)));
        config.revisionAutoScaling().targetUtilizationPercentage()
                .ifPresent(t -> context.add(new ApplyLocalTargetUtilizationPercentageDecorator(name, t)));
        config.revisionAutoScaling().target()
                .ifPresent(t -> context.add(new ApplyLocalAutoscalingTargetDecorator(name, t)));
        config.globalAutoScaling().autoScalerClass()
                .map(AutoScalerClassConverter::convert)
                .ifPresent(a -> {
                    context.add(new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING));
                    context.add(new ApplyGlobalAutoscalingClassDecorator(a));
                });
        config.globalAutoScaling().containerConcurrency().ifPresent(c -> {
            context.add(new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_DEFAULTS, KNATIVE_SERVING));
            context.add(new ApplyGlobalContainerConcurrencyDecorator(c));
        });

        config.globalAutoScaling().requestsPerSecond()
                .ifPresent(r -> {
                    context.add(new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING));
                    context.add(new ApplyGlobalRequestsPerSecondTargetDecorator(r));
                });

        config.globalAutoScaling().targetUtilizationPercentage()
                .ifPresent(t -> {
                    context.add(new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING));
                    context.add(new ApplyGlobalTargetUtilizationDecorator(t));
                });

        if (!config.scaleToZeroEnabled()) {
            context.add(new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING));
            context.add(new AddConfigMapDataDecorator(KNATIVE_CONFIG_AUTOSCALER, "enable-scale-to-zero",
                    String.valueOf(config.scaleToZeroEnabled())));
        }

        context.add(new ApplyServiceTypeDecorator(name, config.serviceType().name()));

        //In Knative its expected that all http ports in probe are omitted (so we set them to null).
        context.add(new ApplyHttpGetActionPortDecorator(name, null));

        //Traffic Splitting
        config.revisionName().ifPresent(r -> context.add(new ApplyRevisionNameDecorator(name, r)));

        config.traffic().forEach((k, traffic) -> {
            //Revision name is K unless we have the edge name of a revision named 'latest' which is not really the latest (in which case use null).
            boolean latestRevision = traffic.latestRevision().get();
            String revisionName = !latestRevision && LATEST_REVISION.equals(k) ? null : k;
            String tag = traffic.tag().orElse(null);
            long percent = traffic.percent().orElse(100L);
            context.add(new ApplyTrafficDecorator(name, revisionName, latestRevision, percent, tag));
        });

        //Add revision decorators
        createVolumeDecorators(context);
        createAppConfigVolumeAndEnvDecorators(context);
        config.hostAliases().entrySet()
                .forEach(e -> context.add(new AddHostAliasesToRevisionDecorator(name, HostAliasConverter.convert(e))));
        config.nodeSelector().ifPresent(n -> context.add(new AddNodeSelectorDecorator(name, n.key(), n.value())));
        config.sidecars().entrySet()
                .forEach(e -> context.add(new AddSidecarToRevisionDecorator(name, ContainerConverter.convert(e))));

        if (!roleBindings.isEmpty()) {
            // no group on purpose?
            context.addToAnyTarget(new ApplyServiceAccountNameToRevisionSpecDecorator());
        }

        //Handle Image Pull Secrets
        config.imagePullSecrets().ifPresent(imagePullSecrets -> {
            String serviceAccountName = config.serviceAccount().orElse(name);
            context.add(new AddServiceAccountResourceDecorator(name));
            context.add(new ApplyServiceAccountToRevisionSpecDecorator(name, serviceAccountName));
            context.add(new AddImagePullSecretToServiceAccountDecorator(serviceAccountName, imagePullSecrets));
        });

        return context.decorators();
    }

    private void createVolumeDecorators(DecoratorsContext context) {
        config.secretVolumes().entrySet()
                .forEach(e -> context.add(new AddSecretVolumeToRevisionDecorator(SecretVolumeConverter.convert(e))));

        config.configMapVolumes().entrySet()
                .forEach(e -> context.add(new AddConfigMapVolumeToRevisionDecorator(ConfigMapVolumeConverter.convert(e))));

        config.emptyDirVolumes().ifPresent(volumes -> volumes.forEach(
                e -> context.add(new AddEmptyDirVolumeToRevisionDecorator(EmptyDirVolumeConverter.convert(e)))));

        config.pvcVolumes().entrySet()
                .forEach(e -> context.add(new AddPvcVolumeToRevisionDecorator(PvcVolumeConverter.convert(e))));

        config.awsElasticBlockStoreVolumes().entrySet().forEach(e -> context.add(
                new AddAwsElasticBlockStoreVolumeToRevisionDecorator(AwsElasticBlockStoreVolumeConverter.convert(e))));

        config.azureFileVolumes().entrySet().forEach(e -> context.add(
                new AddAzureFileVolumeToRevisionDecorator(AzureFileVolumeConverter.convert(e))));

        config.azureDiskVolumes().entrySet().forEach(
                e -> context.add(new AddAzureDiskVolumeToRevisionDecorator(AzureDiskVolumeConverter.convert(e))));
    }

    private void createAppConfigVolumeAndEnvDecorators(DecoratorsContext context) {
        Set<String> paths = new HashSet<>();
        config.appSecret().ifPresent(s -> {
            context.add(new AddSecretVolumeToRevisionDecorator(new SecretVolumeBuilder()
                    .withSecretName(s)
                    .withVolumeName("app-secret")
                    .build()));
            context.add(new AddMountDecorator(new MountBuilder()
                    .withName("app-secret")
                    .withPath("/mnt/app-secret")
                    .build()));
            paths.add("/mnt/app-secret");
        });

        config.appConfigMap().ifPresent(s -> {
            context.add(new AddConfigMapVolumeToRevisionDecorator(new ConfigMapVolumeBuilder()
                    .withConfigMapName(s)
                    .withVolumeName("app-config-map")
                    .build()));
            context.add(new AddMountDecorator(
                    new MountBuilder().withName("app-config-map").withPath("/mnt/app-config-map").build()));
            paths.add("/mnt/app-config-map");
        });

        if (!paths.isEmpty()) {
            context.add(new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, context.name(), new EnvBuilder()
                    .withName("SMALLRYE_CONFIG_LOCATIONS")
                    .withValue(String.join(",", paths))
                    .build()));
        }
    }
}
