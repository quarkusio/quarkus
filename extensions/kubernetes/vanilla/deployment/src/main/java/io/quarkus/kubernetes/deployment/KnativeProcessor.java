package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.project.Project;
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
import io.quarkus.kubernetes.spi.Targetable;

public class KnativeProcessor {

    private static final int KNATIVE_PRIORITY = DEFAULT_PRIORITY;

    private static final String LATEST_REVISION = "latest";
    private static final String KNATIVE_CONFIG_AUTOSCALER = "config-autoscaler";
    private static final String KNATIVE_CONFIG_DEFAULTS = "config-defaults";
    private static final String KNATIVE_SERVING = "knative-serving";
    private static final String KNATIVE_DEV_VISIBILITY = "networking.knative.dev/visibility";

    @BuildStep
    public void checkKnative(ApplicationInfoBuildItem applicationInfo, KnativeConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        List<String> targets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        boolean knativeEnabled = targets.contains(KNATIVE);
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(KNATIVE, KNATIVE_SERVICE, KNATIVE_SERVICE_GROUP,
                        KNATIVE_SERVICE_VERSION, KNATIVE_PRIORITY, knativeEnabled, config.deployStrategy));
        if (knativeEnabled) {
            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(new KubernetesResourceMetadataBuildItem(KNATIVE, KNATIVE_SERVICE_GROUP,
                    KNATIVE_SERVICE_VERSION, KNATIVE_SERVICE, name));
        }
    }

    @BuildStep
    public void createAnnotations(KnativeConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.getAnnotations().forEach((k, v) -> annotations.produce(new KubernetesAnnotationBuildItem(k, v, KNATIVE)));
    }

    @BuildStep
    public void createLabels(KnativeConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config.getLabels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, KNATIVE));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
    }

    @BuildStep
    public void createNamespace(KnativeConfig config, BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        config.getNamespace().ifPresent(n -> namespace.produce(new KubernetesNamespaceBuildItem(KNATIVE, n)));
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

    @BuildStep
    public KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(ApplicationInfoBuildItem applicationInfo,
            KubernetesConfig config, List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        return KubernetesCommonHelper.computeEffectiveServiceAccount(name, KNATIVE,
                config, serviceAccountsFromExtensions,
                decorators);
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
            List<KubernetesEffectiveServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets) {

        List<DecoratorBuildItem> result = new ArrayList<>();
        if (targets.stream().filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .noneMatch(t -> KNATIVE.equals(t.getName()))) {
            return result;
        }

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        final var namespace = Targetable.filteredByTarget(namespaces, KNATIVE, true)
                .findFirst();

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = KubernetesCommonHelper.getPort(ports, config, "http");

        result.addAll(KubernetesCommonHelper.createDecorators(project, KNATIVE, name, namespace, config,
                metricsConfiguration, kubernetesClientConfiguration, annotations,
                labels, image, command, port, livenessPath, readinessPath, startupProbePath,
                roles, clusterRoles, serviceAccounts, roleBindings, clusterRoleBindings));

        image.ifPresent(i -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyContainerImageDecorator(name, i.getImage()))));
        result.add(new DecoratorBuildItem(KNATIVE, new ApplyImagePullPolicyDecorator(name, config.getImagePullPolicy())));

        config.getContainerName().ifPresent(containerName -> result
                .add(new DecoratorBuildItem(KNATIVE, new ChangeContainerNameDecorator(containerName))));

        Stream.concat(config.convertToBuildItems().stream(), Targetable.filteredByTarget(envs, KNATIVE))
                .forEach(e -> result.add(new DecoratorBuildItem(KNATIVE,
                        new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                                .withName(EnvConverter.convertName(e.getName()))
                                .withValue(e.getValue())
                                .withSecret(e.getSecret())
                                .withConfigmap(e.getConfigMap())
                                .withField(e.getField())
                                .build()))));

        if (config.clusterLocal) {
            if (labels.stream().filter(l -> KNATIVE.equals(l.getTarget()))
                    .noneMatch(l -> l.getKey().equals(KNATIVE_DEV_VISIBILITY))) {
                result.add(new DecoratorBuildItem(KNATIVE,
                        new AddLabelDecorator(name, KNATIVE_DEV_VISIBILITY, "cluster-local")));
            }
        }

        config.minScale.ifPresent(min -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyMinScaleDecorator(name, min))));
        config.maxScale.ifPresent(max -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyMaxScaleDecorator(name, max))));
        config.revisionAutoScaling.autoScalerClass.map(AutoScalerClassConverter::convert)
                .ifPresent(a -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalAutoscalingClassDecorator(name, a))));
        config.revisionAutoScaling.metric.map(AutoScalingMetricConverter::convert)
                .ifPresent(m -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalAutoscalingMetricDecorator(name, m))));
        config.revisionAutoScaling.containerConcurrency.ifPresent(
                c -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalContainerConcurrencyDecorator(name, c))));
        config.revisionAutoScaling.targetUtilizationPercentage.ifPresent(t -> result
                .add(new DecoratorBuildItem(KNATIVE, new ApplyLocalTargetUtilizationPercentageDecorator(name, t))));
        config.revisionAutoScaling.target
                .ifPresent(t -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalAutoscalingTargetDecorator(name, t))));
        config.globalAutoScaling.autoScalerClass
                .map(AutoScalerClassConverter::convert)
                .ifPresent(a -> {
                    result.add(
                            new DecoratorBuildItem(KNATIVE,
                                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
                    result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalAutoscalingClassDecorator(a)));
                });
        config.globalAutoScaling.containerConcurrency.ifPresent(c -> {
            result.add(new DecoratorBuildItem(KNATIVE,
                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_DEFAULTS, KNATIVE_SERVING)));
            result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalContainerConcurrencyDecorator(c)));
        });

        config.globalAutoScaling.requestsPerSecond
                .ifPresent(r -> {
                    result.add(
                            new DecoratorBuildItem(KNATIVE,
                                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
                    result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalRequestsPerSecondTargetDecorator(r)));
                });

        config.globalAutoScaling.targetUtilizationPercentage
                .ifPresent(t -> {
                    result.add(
                            new DecoratorBuildItem(KNATIVE,
                                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
                    result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalTargetUtilizationDecorator(t)));
                });

        if (!config.scaleToZeroEnabled) {
            result.add(new DecoratorBuildItem(KNATIVE,
                    new AddConfigMapResourceProvidingDecorator(KNATIVE_CONFIG_AUTOSCALER, KNATIVE_SERVING)));
            result.add(
                    new DecoratorBuildItem(KNATIVE,
                            new AddConfigMapDataDecorator(KNATIVE_CONFIG_AUTOSCALER, "enable-scale-to-zero",
                                    String.valueOf(config.scaleToZeroEnabled))));
        }

        result.add(new DecoratorBuildItem(KNATIVE, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));

        //In Knative its expected that all http ports in probe are omitted (so we set them to null).
        result.add(new DecoratorBuildItem(KNATIVE, new ApplyHttpGetActionPortDecorator(name, null)));

        //Traffic Splitting
        config.revisionName
                .ifPresent(r -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyRevisionNameDecorator(name, r))));

        config.traffic.forEach((k, traffic) -> {
            //Revision name is K unless we have the edge name of a revision named 'latest' which is not really the latest (in which case use null).
            boolean latestRevision = traffic.latestRevision.get();
            String revisionName = !latestRevision && LATEST_REVISION.equals(k) ? null : k;
            String tag = traffic.tag.orElse(null);
            long percent = traffic.percent.orElse(100L);
            result.add(new DecoratorBuildItem(KNATIVE,
                    new ApplyTrafficDecorator(name, revisionName, latestRevision, percent, tag)));
        });

        //Add revision decorators
        result.addAll(createVolumeDecorators(config));
        result.addAll(createAppConfigVolumeAndEnvDecorators(name, config));
        config.getHostAliases().entrySet().forEach(e -> result.add(new DecoratorBuildItem(KNATIVE,
                new AddHostAliasesToRevisionDecorator(name, HostAliasConverter.convert(e)))));
        config.getSidecars().entrySet().forEach(e -> result
                .add(new DecoratorBuildItem(KNATIVE, new AddSidecarToRevisionDecorator(name, ContainerConverter.convert(e)))));

        if (!roleBindings.isEmpty()) {
            result.add(new DecoratorBuildItem(new ApplyServiceAccountNameToRevisionSpecDecorator()));
        }

        //Handle Image Pull Secrets
        config.getImagePullSecrets().ifPresent(imagePullSecrets -> {
            String serviceAccountName = config.getServiceAccount().orElse(name);
            result.add(new DecoratorBuildItem(KNATIVE, new AddServiceAccountResourceDecorator(name)));
            result.add(
                    new DecoratorBuildItem(KNATIVE, new ApplyServiceAccountToRevisionSpecDecorator(name, serviceAccountName)));
            result.add(new DecoratorBuildItem(KNATIVE,
                    new AddImagePullSecretToServiceAccountDecorator(serviceAccountName, imagePullSecrets)));
        });

        return result;
    }

    private static List<DecoratorBuildItem> createVolumeDecorators(PlatformConfiguration config) {
        List<DecoratorBuildItem> result = new ArrayList<>();

        config.getSecretVolumes().entrySet().forEach(e -> result.add(
                new DecoratorBuildItem(KNATIVE, new AddSecretVolumeToRevisionDecorator(SecretVolumeConverter.convert(e)))));

        config.getConfigMapVolumes().entrySet().forEach(e -> result.add(new DecoratorBuildItem(KNATIVE,
                new AddConfigMapVolumeToRevisionDecorator(ConfigMapVolumeConverter.convert(e)))));

        config.getEmptyDirVolumes().forEach(e -> result.add(new DecoratorBuildItem(KNATIVE,
                new AddEmptyDirVolumeToRevisionDecorator(EmptyDirVolumeConverter.convert(e)))));

        config.getPvcVolumes().entrySet().forEach(e -> result
                .add(new DecoratorBuildItem(KNATIVE, new AddPvcVolumeToRevisionDecorator(PvcVolumeConverter.convert(e)))));

        config.getAwsElasticBlockStoreVolumes().entrySet().forEach(e -> result.add(new DecoratorBuildItem(KNATIVE,
                new AddAwsElasticBlockStoreVolumeToRevisionDecorator(AwsElasticBlockStoreVolumeConverter.convert(e)))));

        config.getAzureFileVolumes().entrySet().forEach(e -> result.add(new DecoratorBuildItem(KNATIVE,
                new AddAzureFileVolumeToRevisionDecorator(AzureFileVolumeConverter.convert(e)))));

        config.getAzureDiskVolumes().entrySet().forEach(e -> result.add(new DecoratorBuildItem(KNATIVE,
                new AddAzureDiskVolumeToRevisionDecorator(AzureDiskVolumeConverter.convert(e)))));
        return result;
    }

    private static List<DecoratorBuildItem> createAppConfigVolumeAndEnvDecorators(
            String name,
            PlatformConfiguration config) {

        List<DecoratorBuildItem> result = new ArrayList<>();
        Set<String> paths = new HashSet<>();

        config.getAppSecret().ifPresent(s -> {
            result.add(new DecoratorBuildItem(KNATIVE, new AddSecretVolumeToRevisionDecorator(new SecretVolumeBuilder()
                    .withSecretName(s)
                    .withVolumeName("app-secret")
                    .build())));
            result.add(new DecoratorBuildItem(KNATIVE, new AddMountDecorator(new MountBuilder()
                    .withName("app-secret")
                    .withPath("/mnt/app-secret")
                    .build())));
            paths.add("/mnt/app-secret");
        });

        config.getAppConfigMap().ifPresent(s -> {
            result.add(new DecoratorBuildItem(KNATIVE, new AddConfigMapVolumeToRevisionDecorator(new ConfigMapVolumeBuilder()
                    .withConfigMapName(s)
                    .withVolumeName("app-config-map")
                    .build())));
            result.add(new DecoratorBuildItem(KNATIVE, new AddMountDecorator(new MountBuilder()
                    .withName("app-config-map")
                    .withPath("/mnt/app-config-map")
                    .build())));
            paths.add("/mnt/app-config-map");
        });

        if (!paths.isEmpty()) {
            result.add(new DecoratorBuildItem(KNATIVE,
                    new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                            .withName("SMALLRYE_CONFIG_LOCATIONS")
                            .withValue(String.join(",", paths))
                            .build())));
        }
        return result;
    }

}
