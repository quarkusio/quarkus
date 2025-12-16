package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.INGRESS;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.LIVENESS_PROBE;
import static io.quarkus.kubernetes.deployment.Constants.READINESS_PROBE;
import static io.quarkus.kubernetes.deployment.Constants.STARTUP_PROBE;
import static io.quarkus.kubernetes.deployment.KubernetesCommonHelper.printMessageAboutPortsThatCantChange;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.MANAGEMENT_PORT_NAME;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.managementPortIsEnabled;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.VANILLA_KUBERNETES_PRIORITY;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.DeploymentStrategy;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.IngressBuilder;
import io.dekorate.kubernetes.config.IngressRuleBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.RollingUpdateBuilder;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.AddIngressTlsDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyDeploymentStrategyDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.kubernetes.decorator.ApplyReplicasToDeploymentDecorator;
import io.dekorate.kubernetes.decorator.ApplyReplicasToStatefulSetDecorator;
import io.dekorate.project.Project;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
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
import io.quarkus.kubernetes.spi.Targetable;

public class VanillaKubernetesProcessor extends BaseKubeProcessor<AddPortToKubernetesConfig, KubernetesConfig> {
    private KubernetesConfig config;

    @Override
    protected int priority() {
        return VANILLA_KUBERNETES_PRIORITY;
    }

    @Override
    protected String deploymentTarget() {
        return KUBERNETES;
    }

    @Override
    protected KubernetesConfig config() {
        return config;
    }

    @Override
    protected DeploymentResourceKind deploymentResourceKind(Capabilities capabilities) {
        return config.getDeploymentResourceKind(capabilities);
    }

    @Override
    protected boolean enabled() {
        // when nothing was selected by the user, we enable vanilla Kubernetes by default
        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        final var deploymentTarget = deploymentTarget();
        return userSpecifiedDeploymentTargets.isEmpty() || userSpecifiedDeploymentTargets.contains(deploymentTarget);
    }

    @BuildStep
    public void checkVanillaKubernetes(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities,
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
    protected AddPortToKubernetesConfig portConfigurator(Port port) {
        return new AddPortToKubernetesConfig(port);
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(List<KubernetesPortBuildItem> ports) {
        final var result = super.createConfigurators(ports);
        if (config.ingress() != null) {
            result.add(new ConfiguratorBuildItem(new ApplyKubernetesIngressConfigurator((config.ingress()))));
        }

        // Handle remote debug configuration for container ports
        if (config.remoteDebug().enabled()) {
            result.add(new ConfiguratorBuildItem(new AddPortToKubernetesConfig(config.remoteDebug().buildDebugPort())));
        }

        return result;
    }

    @BuildStep
    public KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(ApplicationInfoBuildItem applicationInfo,
            List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        return super.computeEffectiveServiceAccounts(applicationInfo, serviceAccountsFromExtensions, decorators);
    }

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, Capabilities capabilities,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            List<KubernetesNamespaceBuildItem> namespaces,
            List<KubernetesJobBuildItem> jobs,
            List<KubernetesInitContainerBuildItem> initContainers,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels, List<KubernetesEnvBuildItem> envs,
            Optional<ContainerImageInfoBuildItem> image, Optional<KubernetesCommandBuildItem> command,
            Optional<KubernetesProbePortNameBuildItem> portName,
            List<KubernetesPortBuildItem> ports, Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesClusterRoleBuildItem> clusterRoles,
            List<KubernetesEffectiveServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets) {
        final var clusterKind = deploymentTarget();
        final var config = config();
        final List<DecoratorBuildItem> result = new ArrayList<>();
        if (targets.stream().filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .noneMatch(t -> clusterKind.equals(t.getName()))) {
            return result;
        }
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        final var namespace = Targetable.filteredByTarget(namespaces, clusterKind, true)
                .findFirst();

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = KubernetesCommonHelper.getPort(ports, config);

        result.addAll(KubernetesCommonHelper.createDecorators(project, clusterKind, name, namespace, config,
                metricsConfiguration, kubernetesClientConfiguration, annotations, labels, image, command, port,
                livenessPath, readinessPath, startupPath,
                roles, clusterRoles, serviceAccounts, roleBindings, clusterRoleBindings));

        DeploymentResourceKind deploymentKind = config.getDeploymentResourceKind(capabilities);
        if (deploymentKind != DeploymentResourceKind.Deployment) {
            result.add(new DecoratorBuildItem(clusterKind, new RemoveDeploymentResourceDecorator(name)));
        }
        if (deploymentKind == DeploymentResourceKind.StatefulSet) {
            result.add(new DecoratorBuildItem(clusterKind, new AddStatefulSetResourceDecorator(name, config)));
        } else if (deploymentKind == DeploymentResourceKind.Job) {
            result.add(new DecoratorBuildItem(clusterKind, new AddJobResourceDecorator(name, config.job())));
        } else if (deploymentKind == DeploymentResourceKind.CronJob) {
            result.add(new DecoratorBuildItem(clusterKind, new AddCronJobResourceDecorator(name, config.cronJob())));
        }

        if (config.ingress() != null) {
            if (config.ingress().tls() != null) {
                for (Map.Entry<String, IngressConfig.IngressTlsConfig> tlsConfigEntry : config.ingress().tls().entrySet()) {
                    if (tlsConfigEntry.getValue().enabled()) {
                        String[] tlsHosts = tlsConfigEntry.getValue().hosts()
                                .map(l -> l.toArray(new String[0]))
                                .orElse(null);
                        result.add(new DecoratorBuildItem(clusterKind,
                                new AddIngressTlsDecorator(name, new IngressBuilder()
                                        .withTlsSecretName(tlsConfigEntry.getKey())
                                        .withTlsHosts(tlsHosts)
                                        .build())));
                    }
                }

            }
            for (Map.Entry<String, String> annotation : config.ingress().annotations().entrySet()) {
                result.add(new DecoratorBuildItem(clusterKind,
                        new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), INGRESS)));
            }

            for (IngressConfig.IngressRuleConfig rule : config.ingress().rules().values()) {
                result.add(new DecoratorBuildItem(clusterKind, new AddIngressRuleDecorator(name, port,
                        new IngressRuleBuilder()
                                .withHost(rule.host())
                                .withPath(rule.path())
                                .withPathType(rule.pathType())
                                .withServiceName(rule.serviceName().orElse(null))
                                .withServicePortName(rule.servicePortName().orElse(null))
                                .withServicePortNumber(rule.servicePortNumber().orElse(-1))
                                .build())));
            }
        }

        if (config.replicas() != 1) {
            // This only affects Deployment
            result.add(new DecoratorBuildItem(clusterKind, new ApplyReplicasToDeploymentDecorator(name, config.replicas())));
            // This only affects StatefulSet
            result.add(new DecoratorBuildItem(clusterKind, new ApplyReplicasToStatefulSetDecorator(name, config.replicas())));
        }

        image.ifPresent(
                i -> result.add(new DecoratorBuildItem(clusterKind, new ApplyContainerImageDecorator(name, i.getImage()))));

        result.add(new DecoratorBuildItem(clusterKind, new ApplyImagePullPolicyDecorator(name, config.imagePullPolicy())));
        result.add(new DecoratorBuildItem(clusterKind, new AddSelectorToDeploymentDecorator(name)));

        var stream = Stream.concat(config.convertToBuildItems().stream(), Targetable.filteredByTarget(envs, clusterKind));
        if (config.idempotent()) {
            stream = stream.sorted(Comparator.comparing(e -> EnvConverter.convertName(e.getName())));
        }
        stream.forEach(e -> result.add(new DecoratorBuildItem(clusterKind,
                new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                        new EnvBuilder().withName(EnvConverter.convertName(e.getName())).withValue(e.getValue())
                                .withSecret(e.getSecret()).withConfigmap(e.getConfigMap()).withField(e.getField())
                                .withPrefix(e.getPrefix())
                                .build()))));

        config.containerName().ifPresent(containerName -> result
                .add(new DecoratorBuildItem(clusterKind, new ChangeContainerNameDecorator(containerName))));

        // Service handling
        result.add(new DecoratorBuildItem(clusterKind, new ApplyServiceTypeDecorator(name, config.serviceType().name())));
        if ((config.serviceType() == ServiceType.NodePort)) {
            List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports().entrySet().stream()
                    .filter(e -> e.getValue().nodePort().isPresent())
                    .toList();
            if (!nodeConfigPorts.isEmpty()) {
                for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                    result.add(new DecoratorBuildItem(clusterKind,
                            new AddNodePortDecorator(name, entry.getValue().nodePort().getAsInt(), entry.getKey())));
                }
            } else if (config.nodePort().isPresent()) {
                result.add(new DecoratorBuildItem(clusterKind,
                        new AddNodePortDecorator(name, config.nodePort().getAsInt(), config.ingress().targetPort())));
            }
        }

        // Probe port handling
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

        // Handle remote debug configuration
        if (config.remoteDebug().enabled()) {
            result.add(new DecoratorBuildItem(clusterKind, new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                    config.remoteDebug().buildJavaToolOptionsEnv())));
        }

        // Handle init Containers and Jobs
        result.addAll(KubernetesCommonHelper.createInitContainerDecorators(clusterKind, name, initContainers, result));
        result.addAll(KubernetesCommonHelper.createInitJobDecorators(clusterKind, name, jobs, result));

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        if (managementPortIsEnabled()
                && (config.ingress() == null
                        || !config.ingress().expose()
                        || !config.ingress().targetPort().equals(MANAGEMENT_PORT_NAME))) {
            result.add(new DecoratorBuildItem(clusterKind, new RemovePortFromServiceDecorator(name, MANAGEMENT_PORT_NAME)));
        }

        // Handle deployment strategy
        if (config.strategy() != DeploymentStrategy.None) {
            result.add(new DecoratorBuildItem(clusterKind,
                    new ApplyDeploymentStrategyDecorator(name, config.strategy(), new RollingUpdateBuilder()
                            .withMaxSurge(config.rollingUpdate().maxSurge())
                            .withMaxUnavailable(config.rollingUpdate().maxUnavailable())
                            .build())));
        }
        printMessageAboutPortsThatCantChange(clusterKind, ports, config);
        return result;
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
