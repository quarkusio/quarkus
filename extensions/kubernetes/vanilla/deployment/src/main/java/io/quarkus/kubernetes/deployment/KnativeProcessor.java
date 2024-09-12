package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.ArrayList;
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

public class KnativeProcessor extends BaseProcessor<KnativeConfig> {

    private static final int KNATIVE_PRIORITY = DEFAULT_PRIORITY;

    private static final String LATEST_REVISION = "latest";
    private static final String KNATIVE_CONFIG_AUTOSCALER = "config-autoscaler";
    private static final String KNATIVE_CONFIG_DEFAULTS = "config-defaults";
    private static final String KNATIVE_SERVING = "knative-serving";
    private static final String KNATIVE_DEV_VISIBILITY = "networking.knative.dev/visibility";

    public KnativeProcessor() {
        super(KNATIVE, KNATIVE_PRIORITY);
    }

    @BuildStep
    public void checkKnative(ApplicationInfoBuildItem applicationInfo, KnativeConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        doCheckEnabled(applicationInfo, null, config, deploymentTargets, resourceMeta);
    }

    @BuildStep
    public void createAnnotations(KnativeConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        doCreateAnnotations(config, annotations);
    }

    @BuildStep
    public void createLabels(KnativeConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        doCreateLabels(config, labels, imageLabels);
    }

    @BuildStep
    public void createNamespace(KnativeConfig config, BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        doCreateNamespace(config, namespace);
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(KnativeConfig config, List<KubernetesPortBuildItem> ports) {
        List<ConfiguratorBuildItem> result = new ArrayList<>();
        KubernetesCommonHelper.combinePorts(ports, config).values()
                .stream()
                // At the moment, Knative only supports single port binding: https://github.com/knative/serving/issues/8471
                .filter(p -> p.getName().equals("http"))
                .findFirst()
                .ifPresent(value -> result.add(new ConfiguratorBuildItem(new AddPortToKnativeConfig(value))));
        return result;
    }

    @Override
    protected Optional<Port> createPort(KnativeConfig config, List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config, "http");
    }

    @Override
    protected void replicasDecorators(KnativeConfig config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            KnativeConfig config,
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
            List<KubernetesDeploymentTargetBuildItem> targets,
            BuildProducer<KubernetesEffectiveServiceAccountBuildItem> serviceAccountProducer) {

        final var manifestInfo = doCreateDecorators(applicationInfo, outputTarget, config, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, annotations, labels, envs, image, command, ports, livenessPath,
                readinessPath, startupProbePath, roles, clusterRoles, serviceAccounts, roleBindings, customProjectRoot,
                targets);

        if (manifestInfo.skipFurtherProcessing()) {
            return manifestInfo.getDecoratorsAndProduceServiceAccountBuildItem(serviceAccountProducer);
        }

        containerImageDecorators(config, manifestInfo, image);

        final var name = manifestInfo.getDefaultName();
        if (config.clusterLocal) {
            if (labels.stream().filter(l -> flavor.equals(l.getTarget()))
                    .noneMatch(l -> l.getKey().equals(KNATIVE_DEV_VISIBILITY))) {
                manifestInfo.add(new DecoratorBuildItem(flavor,
                        new AddLabelDecorator(name, KNATIVE_DEV_VISIBILITY, "cluster-local")));
            }
        }

        config.minScale
                .ifPresent(min -> manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyMinScaleDecorator(name, min))));
        config.maxScale
                .ifPresent(max -> manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyMaxScaleDecorator(name, max))));
        config.revisionAutoScaling.autoScalerClass.map(AutoScalerClassConverter::convert)
                .ifPresent(a -> manifestInfo
                        .add(new DecoratorBuildItem(flavor, new ApplyLocalAutoscalingClassDecorator(name, a))));
        config.revisionAutoScaling.metric.map(AutoScalingMetricConverter::convert)
                .ifPresent(m -> manifestInfo
                        .add(new DecoratorBuildItem(flavor, new ApplyLocalAutoscalingMetricDecorator(name, m))));
        config.revisionAutoScaling.containerConcurrency.ifPresent(
                c -> manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyLocalContainerConcurrencyDecorator(name, c))));
        config.revisionAutoScaling.targetUtilizationPercentage.ifPresent(t -> manifestInfo
                .add(new DecoratorBuildItem(flavor, new ApplyLocalTargetUtilizationPercentageDecorator(name, t))));
        config.revisionAutoScaling.target
                .ifPresent(t -> manifestInfo
                        .add(new DecoratorBuildItem(flavor, new ApplyLocalAutoscalingTargetDecorator(name, t))));
        config.globalAutoScaling.autoScalerClass
                .map(AutoScalerClassConverter::convert)
                .ifPresent(a -> {
                    manifestInfo.add(
                            new DecoratorBuildItem(flavor,
                                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
                    manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyGlobalAutoscalingClassDecorator(a)));
                });
        config.globalAutoScaling.containerConcurrency.ifPresent(c -> {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_DEFAULTS, KNATIVE_SERVING)));
            manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyGlobalContainerConcurrencyDecorator(c)));
        });

        config.globalAutoScaling.requestsPerSecond
                .ifPresent(r -> {
                    manifestInfo.add(
                            new DecoratorBuildItem(flavor,
                                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
                    manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyGlobalRequestsPerSecondTargetDecorator(r)));
                });

        config.globalAutoScaling.targetUtilizationPercentage
                .ifPresent(t -> {
                    manifestInfo.add(
                            new DecoratorBuildItem(flavor,
                                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
                    manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyGlobalTargetUtilizationDecorator(t)));
                });

        if (!config.scaleToZeroEnabled) {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
            manifestInfo.add(
                    new DecoratorBuildItem(flavor,
                            new AddConfigMapDataDecorator(KNATIVE_CONFIG_AUTOSCALER, "enable-scale-to-zero",
                                    String.valueOf(config.scaleToZeroEnabled))));
        }

        manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));

        //In Knative its expected that all http ports in probe are omitted (so we set them to null).
        manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyHttpGetActionPortDecorator(name, (Integer) null)));

        //Traffic Splitting
        config.revisionName.ifPresent(r -> {
            manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyRevisionNameDecorator(name, r)));
        });

        config.traffic.forEach((k, v) -> {
            TrafficConfig traffic = v;
            //Revision name is K unless we have the edge name of a revision named 'latest' which is not really the latest (in which case use null).
            boolean latestRevision = traffic.latestRevision.get();
            String revisionName = !latestRevision && LATEST_REVISION.equals(k) ? null : k;
            String tag = traffic.tag.orElse(null);
            long percent = traffic.percent.orElse(100L);
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new ApplyTrafficDecorator(name, revisionName, latestRevision, percent, tag)));
        });

        //Add revision decorators
        manifestInfo.addAll(createVolumeDecorators(config));
        manifestInfo.addAll(createAppConfigVolumeAndEnvDecorators(name, config));
        config.getHostAliases().entrySet().forEach(e -> {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new AddHostAliasesToRevisionDecorator(name, HostAliasConverter.convert(e))));
        });
        config.getSidecars().entrySet().forEach(e -> {
            manifestInfo.add(
                    new DecoratorBuildItem(flavor, new AddSidecarToRevisionDecorator(name, ContainerConverter.convert(e))));
        });

        if (!roleBindings.isEmpty()) {
            manifestInfo.add(new DecoratorBuildItem(new ApplyServiceAccountNameToRevisionSpecDecorator()));
        }

        //Handle Image Pull Secrets
        config.getImagePullSecrets().ifPresent(imagePullSecrets -> {
            String serviceAccountName = config.getServiceAccount().orElse(name);
            manifestInfo.add(new DecoratorBuildItem(flavor, new AddServiceAccountResourceDecorator(name)));
            manifestInfo.add(
                    new DecoratorBuildItem(flavor, new ApplyServiceAccountToRevisionSpecDecorator(name, serviceAccountName)));
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new AddImagePullSecretToServiceAccountDecorator(serviceAccountName, imagePullSecrets)));
        });

        return manifestInfo.getDecoratorsAndProduceServiceAccountBuildItem(serviceAccountProducer);
    }

    private List<DecoratorBuildItem> createVolumeDecorators(PlatformConfiguration config) {
        List<DecoratorBuildItem> result = new ArrayList<>();

        config.getSecretVolumes().entrySet().forEach(e -> {
            result.add(
                    new DecoratorBuildItem(flavor, new AddSecretVolumeToRevisionDecorator(SecretVolumeConverter.convert(e))));
        });

        config.getConfigMapVolumes().entrySet().forEach(e -> {
            result.add(new DecoratorBuildItem(flavor,
                    new AddConfigMapVolumeToRevisionDecorator(ConfigMapVolumeConverter.convert(e))));
        });

        config.getEmptyDirVolumes().forEach(e -> {
            result.add(new DecoratorBuildItem(flavor,
                    new AddEmptyDirVolumeToRevisionDecorator(EmptyDirVolumeConverter.convert(e))));
        });

        config.getPvcVolumes().entrySet().forEach(e -> {
            result.add(new DecoratorBuildItem(flavor, new AddPvcVolumeToRevisionDecorator(PvcVolumeConverter.convert(e))));
        });

        config.getAwsElasticBlockStoreVolumes().entrySet().forEach(e -> {
            result.add(new DecoratorBuildItem(flavor,
                    new AddAwsElasticBlockStoreVolumeToRevisionDecorator(AwsElasticBlockStoreVolumeConverter.convert(e))));
        });

        config.getAzureFileVolumes().entrySet().forEach(e -> {
            result.add(new DecoratorBuildItem(flavor,
                    new AddAzureFileVolumeToRevisionDecorator(AzureFileVolumeConverter.convert(e))));
        });

        config.getAzureDiskVolumes().entrySet().forEach(e -> {
            result.add(new DecoratorBuildItem(flavor,
                    new AddAzureDiskVolumeToRevisionDecorator(AzureDiskVolumeConverter.convert(e))));
        });
        return result;
    }

    private List<DecoratorBuildItem> createAppConfigVolumeAndEnvDecorators(String name,
            PlatformConfiguration config) {

        List<DecoratorBuildItem> result = new ArrayList<>();
        Set<String> paths = new HashSet<>();

        config.getAppSecret().ifPresent(s -> {
            result.add(new DecoratorBuildItem(flavor, new AddSecretVolumeToRevisionDecorator(new SecretVolumeBuilder()
                    .withSecretName(s)
                    .withVolumeName("app-secret")
                    .build())));
            result.add(new DecoratorBuildItem(flavor, new AddMountDecorator(new MountBuilder()
                    .withName("app-secret")
                    .withPath("/mnt/app-secret")
                    .build())));
            paths.add("/mnt/app-secret");
        });

        config.getAppConfigMap().ifPresent(s -> {
            result.add(new DecoratorBuildItem(flavor, new AddConfigMapVolumeToRevisionDecorator(new ConfigMapVolumeBuilder()
                    .withConfigMapName(s)
                    .withVolumeName("app-config-map")
                    .build())));
            result.add(new DecoratorBuildItem(flavor, new AddMountDecorator(new MountBuilder()
                    .withName("app-config-map")
                    .withPath("/mnt/app-config-map")
                    .build())));
            paths.add("/mnt/app-config-map");
        });

        if (!paths.isEmpty()) {
            result.add(new DecoratorBuildItem(flavor,
                    new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                            .withName("SMALLRYE_CONFIG_LOCATIONS")
                            .withValue(String.join(",", paths))
                            .build())));
        }
        return result;
    }

}
