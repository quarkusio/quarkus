package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
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
import io.quarkus.kubernetes.spi.Targetable;

public abstract class BaseKubeProcessor<P, C extends PlatformConfiguration> {

    protected abstract int priority();

    protected abstract String deploymentTarget();

    protected abstract P portConfigurator(Port port);

    protected abstract C config();

    protected abstract Optional<Port> optionalPort(List<KubernetesPortBuildItem> ports);

    protected String clusterType() {
        return KUBERNETES;
    }

    protected boolean enabled() {
        return true;
    }

    protected DeploymentResourceKind deploymentResourceKind(Capabilities capabilities) {
        return DeploymentResourceKind.Deployment;
    }

    protected void produceDeploymentBuildItem(ApplicationInfoBuildItem applicationInfo,
            Capabilities capabilities,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        final var enabled = enabled();
        final var config = config();
        final var drk = deploymentResourceKind(capabilities);
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(deploymentTarget(), drk.getKind(), drk.getGroup(), drk.getVersion(),
                        priority(), enabled, config.deployStrategy()));

        if (enabled) {
            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(
                    new KubernetesResourceMetadataBuildItem(clusterType(), drk.getGroup(), drk.getVersion(), drk.getKind(),
                            name));
        }
    }

    protected void createAnnotations(BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config().annotations()
                .forEach((k, v) -> annotations.produce(new KubernetesAnnotationBuildItem(k, v, deploymentTarget())));
    }

    protected void createLabels(BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config().labels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, deploymentTarget()));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
        labels.produce(
                new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", deploymentTarget()));
    }

    protected Stream<Port> asStream(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.combinePorts(ports, config()).values().stream();
    }

    protected List<ConfiguratorBuildItem> createConfigurators(List<KubernetesPortBuildItem> ports) {
        return asStream(ports)
                .map(port -> new ConfiguratorBuildItem(portConfigurator(port)))
                .collect(Collectors.toCollection(ArrayList::new)); // need a mutable list so sub-classes can add to it
    }

    protected KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(
            ApplicationInfoBuildItem applicationInfo,
            List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        final var config = config();
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        return KubernetesCommonHelper.computeEffectiveServiceAccount(name, deploymentTarget(),
                config, serviceAccountsFromExtensions,
                decorators);
    }

    protected void createNamespace(BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        config().namespace().ifPresent(n -> namespace.produce(new KubernetesNamespaceBuildItem(deploymentTarget(), n)));
    }

    protected boolean isDeploymentTargetDisabled(List<KubernetesDeploymentTargetBuildItem> targets) {
        return targets.stream()
                .filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .noneMatch(t -> deploymentTarget().equals(t.getName()));
    }

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
        final var config = config();
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        if (config.externalizeInit()) {
            InitTaskProcessor.process(deploymentTarget(), name, image, initTasks, config.initTaskDefaults(), config.initTasks(),
                    jobs, initContainers, env, roles, roleBindings, serviceAccount, decorators);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected List<DecoratorBuildItem> commonDecorators(
            ApplicationInfoBuildItem applicationInfo,
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
            Optional<CustomProjectRootBuildItem> customProjectRoot) {
        final var clusterKind = deploymentTarget();
        final var config = config();
        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        final var namespace = Targetable.filteredByTarget(namespaces, clusterType(), true)
                .findFirst();

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = optionalPort(ports);

        List<DecoratorBuildItem> result = new ArrayList<>(
                KubernetesCommonHelper.createDecorators(project, clusterKind, name, namespace, config,
                        metricsConfiguration, kubernetesClientConfiguration,
                        annotations, labels, image, command,
                        port, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                        clusterRoleBindings));

        result.add(new DecoratorBuildItem(clusterKind, new ApplyImagePullPolicyDecorator(name, pullPolicy())));
        image.ifPresent(
                i -> result.add(new DecoratorBuildItem(clusterKind, new ApplyContainerImageDecorator(name, i.getImage()))));

        var stream = Stream.concat(config.convertToBuildItems().stream(), Targetable.filteredByTarget(envs, clusterType()));
        if (config.idempotent()) {
            stream = stream.sorted(Comparator.comparing(e -> EnvConverter.convertName(e.getName())));
        }
        stream.forEach(e -> result.add(new DecoratorBuildItem(clusterKind,
                new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                        .withName(EnvConverter.convertName(e.getName()))
                        .withValue(e.getValue())
                        .withSecret(e.getSecret())
                        .withConfigmap(e.getConfigMap())
                        .withField(e.getField())
                        .withPrefix(e.getPrefix())
                        .build()))));

        return result;
    }

    protected void initTasks(List<KubernetesInitContainerBuildItem> initContainers, List<KubernetesJobBuildItem> jobs,
            List<DecoratorBuildItem> result, String name) {
        final var clusterKind = deploymentTarget();
        result.addAll(KubernetesCommonHelper.createInitContainerDecorators(clusterKind, name, initContainers, result));
        result.addAll(KubernetesCommonHelper.createInitJobDecorators(clusterKind, name, jobs, result));
    }

    protected void probes(List<KubernetesPortBuildItem> ports, Optional<KubernetesProbePortNameBuildItem> portName,
            List<DecoratorBuildItem> result, String name) {
        final var clusterKind = deploymentTarget();
        final var config = config();
        result.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, clusterKind, LIVENESS_PROBE, config.livenessProbe(),
                        portName,
                        ports,
                        config.ports()));
        result.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, clusterKind, READINESS_PROBE, config.readinessProbe(),
                        portName,
                        ports,
                        config.ports()));
        result.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, clusterKind, STARTUP_PROBE, config.startupProbe(),
                        portName,
                        ports,
                        config.ports()));
    }

    protected ImagePullPolicy pullPolicy() {
        return config().imagePullPolicy();
    }
}
