package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.decorator.AddConfigMapDataDecorator;
import io.dekorate.kubernetes.decorator.AddConfigMapResourceProvidingDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.project.Project;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public class KnativeProcessor {

    private static final int KNATIVE_PRIORITY = DEFAULT_PRIORITY;

    @BuildStep
    public void checkKnative(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        List<String> targets = KubernetesConfigUtil.getUserSpecifiedDeploymentTargets();
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(KNATIVE, DEPLOYMENT, KNATIVE_PRIORITY, targets.contains(KNATIVE)));
    }

    @BuildStep
    public void createAnnotations(KnativeConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.getAnnotations().forEach((k, v) -> {
            annotations.produce(new KubernetesAnnotationBuildItem(k, v, KNATIVE));
        });
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
    public List<ConfiguratorBuildItem> createConfigurators(KnativeConfig config, List<KubernetesPortBuildItem> ports) {
        List<ConfiguratorBuildItem> result = new ArrayList<>();
        result.addAll(KubernetesCommonHelper.createPlatformConfigurators(config));
        result.addAll(KubernetesCommonHelper.createGlobalConfigurators(ports));
        return result;

    }

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            KnativeConfig config,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            List<KubernetesEnvBuildItem> envs,
            Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            List<KubernetesPortBuildItem> ports,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesRoleBindingBuildItem> roleBindings) {

        List<DecoratorBuildItem> result = new ArrayList<>();
        String name = ResourceNameUtil.getResourceName(config, applicationInfo);

        Project project = KubernetesCommonHelper.createProject(applicationInfo, outputTarget, packageConfig);
        result.addAll(KubernetesCommonHelper.createDecorators(project, KNATIVE, name, config, metricsConfiguration, annotations,
                labels, command,
                ports, livenessPath, readinessPath, roles, roleBindings));

        image.ifPresent(i -> {
            result.add(new DecoratorBuildItem(KNATIVE, new ApplyContainerImageDecorator(name, i.getImage())));
        });

        Stream.concat(config.convertToBuildItems().stream(),
                envs.stream().filter(e -> e.getTarget() == null || KNATIVE.equals(e.getTarget()))).forEach(e -> {
                    result.add(new DecoratorBuildItem(KNATIVE,
                            new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                                    .withName(EnvConverter.convertName(e.getName()))
                                    .withValue(e.getValue())
                                    .withSecret(e.getSecret())
                                    .withConfigmap(e.getConfigMap())
                                    .withField(e.getField())
                                    .build())));
                });

        if (config.clusterLocal) {
            result.add(new DecoratorBuildItem(KNATIVE,
                    new AddLabelDecorator(name, "serving.knative.dev/visibility", "cluster-local")));
        }

        config.minScale.ifPresent(min -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyMinScaleDecorator(name, min))));

        config.maxScale.ifPresent(max -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyMaxScaleDecorator(name, max))));

        config.revisionAutoScaling.autoScalerClass.map(AutoScalerClassConverter::convert)
                .ifPresent(a -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalAutoscalingClassDecorator(name, a))));

        config.revisionAutoScaling.metric.map(AutoScalingMetricConverter::convert)
                .ifPresent(m -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalAutoscalingMetricDecorator(name, m))));

        config.revisionAutoScaling.containerConcurrency
                .ifPresent(
                        c -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalContainerConcurrencyDecorator(name, c))));

        config.revisionAutoScaling.targetUtilizationPercentage
                .ifPresent(t -> result
                        .add(new DecoratorBuildItem(KNATIVE, new ApplyLocalTargetUtilizationPercentageDecorator(name, t))));
        config.revisionAutoScaling.target
                .ifPresent(t -> result.add(new DecoratorBuildItem(KNATIVE, new ApplyLocalAutoscalingTargetDecorator(name, t))));

        config.globalAutoScaling.autoScalerClass
                .map(AutoScalerClassConverter::convert)
                .ifPresent(a -> {
                    result.add(
                            new DecoratorBuildItem(KNATIVE, new AddConfigMapResourceProvidingDecorator("config-autoscaler")));
                    result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalAutoscalingClassDecorator(a)));
                });

        config.globalAutoScaling.containerConcurrency
                .ifPresent(c -> {
                    result.add(new DecoratorBuildItem(KNATIVE, new AddConfigMapResourceProvidingDecorator("config-defaults")));
                    result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalContainerConcurrencyDecorator(c)));
                });

        config.globalAutoScaling.requestsPerSecond
                .ifPresent(r -> {
                    result.add(
                            new DecoratorBuildItem(KNATIVE, new AddConfigMapResourceProvidingDecorator("config-autoscaler")));
                    result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalRequestsPerSecondTargetDecorator(r)));
                });

        config.globalAutoScaling.targetUtilizationPercentage
                .ifPresent(t -> {
                    result.add(
                            new DecoratorBuildItem(KNATIVE, new AddConfigMapResourceProvidingDecorator("config-autoscaler")));
                    result.add(new DecoratorBuildItem(KNATIVE, new ApplyGlobalTargetUtilizationDecorator(t)));
                });

        if (!config.scaleToZeroEnabled) {
            result.add(new DecoratorBuildItem(KNATIVE, new AddConfigMapResourceProvidingDecorator("config-autoscaler")));
            result.add(
                    new DecoratorBuildItem(KNATIVE, new AddConfigMapDataDecorator("config-autoscaler", "enable-scale-to-zero",
                            String.valueOf(config.scaleToZeroEnabled))));
        }

        result.add(new DecoratorBuildItem(KNATIVE, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));

        //In Knative its expected that all http ports in probe are ommitted (so we set them to null).
        result.add(new DecoratorBuildItem(KNATIVE, new ApplyHttpGetActionPortDecorator(null)));
        return result;
    }
}
