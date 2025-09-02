
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

public class VanillaKubernetesProcessor {

    @BuildStep
    public void checkVanillaKubernetes(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities,
            KubernetesConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        DeploymentResourceKind deploymentResourceKind = config.getDeploymentResourceKind(capabilities);

        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        if (userSpecifiedDeploymentTargets.isEmpty() || userSpecifiedDeploymentTargets.contains(KUBERNETES)) {
            // when nothing was selected by the user, we enable vanilla Kubernetes by default
            deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(KUBERNETES,
                    deploymentResourceKind.getKind(), deploymentResourceKind.getGroup(), deploymentResourceKind.getVersion(),
                    VANILLA_KUBERNETES_PRIORITY, true, config.deployStrategy()));

            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(new KubernetesResourceMetadataBuildItem(KUBERNETES, deploymentResourceKind.getGroup(),
                    deploymentResourceKind.getVersion(), deploymentResourceKind.getKind(), name));

        } else {
            deploymentTargets
                    .produce(new KubernetesDeploymentTargetBuildItem(KUBERNETES, deploymentResourceKind.getKind(),
                            deploymentResourceKind.getGroup(),
                            deploymentResourceKind.getVersion(), VANILLA_KUBERNETES_PRIORITY, false, config.deployStrategy()));
        }
    }

    @BuildStep
    public void createAnnotations(KubernetesConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.annotations().forEach((k, v) -> annotations.produce(new KubernetesAnnotationBuildItem(k, v, KUBERNETES)));
    }

    @BuildStep
    public void createLabels(KubernetesConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config.labels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, KUBERNETES));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
        labels.produce(new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", KUBERNETES));
    }

    @BuildStep
    public void createNamespace(KubernetesConfig config, BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        config.namespace().ifPresent(n -> namespace.produce(new KubernetesNamespaceBuildItem(KUBERNETES, n)));
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(KubernetesConfig config, List<KubernetesPortBuildItem> ports) {
        List<ConfiguratorBuildItem> result = new ArrayList<>();
        KubernetesCommonHelper.combinePorts(ports, config).values()
                .forEach(value -> result.add(new ConfiguratorBuildItem(new AddPortToKubernetesConfig(value))));
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
            KubernetesConfig config, List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        return KubernetesCommonHelper.computeEffectiveServiceAccount(name, KUBERNETES,
                config, serviceAccountsFromExtensions,
                decorators);
    }

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, Capabilities capabilities, KubernetesConfig config,
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

        final List<DecoratorBuildItem> result = new ArrayList<>();
        if (targets.stream().filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .noneMatch(t -> KUBERNETES.equals(t.getName()))) {
            return result;
        }
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        final var namespace = Targetable.filteredByTarget(namespaces, KUBERNETES, true)
                .findFirst();

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = KubernetesCommonHelper.getPort(ports, config);
        result.addAll(KubernetesCommonHelper.createDecorators(project, KUBERNETES, name, namespace, config,
                metricsConfiguration, kubernetesClientConfiguration, annotations, labels, image, command, port,
                livenessPath, readinessPath, startupPath,
                roles, clusterRoles, serviceAccounts, roleBindings, clusterRoleBindings));

        DeploymentResourceKind deploymentKind = config.getDeploymentResourceKind(capabilities);
        if (deploymentKind != DeploymentResourceKind.Deployment) {
            result.add(new DecoratorBuildItem(KUBERNETES, new RemoveDeploymentResourceDecorator(name)));
        }
        if (deploymentKind == DeploymentResourceKind.StatefulSet) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddStatefulSetResourceDecorator(name, config)));
        } else if (deploymentKind == DeploymentResourceKind.Job) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddJobResourceDecorator(name, config.job())));
        } else if (deploymentKind == DeploymentResourceKind.CronJob) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddCronJobResourceDecorator(name, config.cronJob())));
        }

        if (config.ingress() != null) {
            if (config.ingress().tls() != null) {
                for (Map.Entry<String, IngressConfig.IngressTlsConfig> tlsConfigEntry : config.ingress().tls().entrySet()) {
                    if (tlsConfigEntry.getValue().enabled()) {
                        String[] tlsHosts = tlsConfigEntry.getValue().hosts()
                                .map(l -> l.toArray(new String[0]))
                                .orElse(null);
                        result.add(new DecoratorBuildItem(KUBERNETES,
                                new AddIngressTlsDecorator(name, new IngressBuilder()
                                        .withTlsSecretName(tlsConfigEntry.getKey())
                                        .withTlsHosts(tlsHosts)
                                        .build())));
                    }
                }

            }
            for (Map.Entry<String, String> annotation : config.ingress().annotations().entrySet()) {
                result.add(new DecoratorBuildItem(KUBERNETES,
                        new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), INGRESS)));
            }

            for (IngressConfig.IngressRuleConfig rule : config.ingress().rules().values()) {
                result.add(new DecoratorBuildItem(KUBERNETES, new AddIngressRuleDecorator(name, port,
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
            result.add(new DecoratorBuildItem(KUBERNETES, new ApplyReplicasToDeploymentDecorator(name, config.replicas())));
            // This only affects StatefulSet
            result.add(new DecoratorBuildItem(KUBERNETES, new ApplyReplicasToStatefulSetDecorator(name, config.replicas())));
        }

        image.ifPresent(
                i -> result.add(new DecoratorBuildItem(KUBERNETES, new ApplyContainerImageDecorator(name, i.getImage()))));

        result.add(new DecoratorBuildItem(KUBERNETES, new ApplyImagePullPolicyDecorator(name, config.imagePullPolicy())));
        result.add(new DecoratorBuildItem(KUBERNETES, new AddSelectorToDeploymentDecorator(name)));

        var stream = Stream.concat(config.convertToBuildItems().stream(), Targetable.filteredByTarget(envs, KUBERNETES));
        if (config.idempotent()) {
            stream = stream.sorted(Comparator.comparing(e -> EnvConverter.convertName(e.getName())));
        }
        stream.forEach(e -> result.add(new DecoratorBuildItem(KUBERNETES,
                new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                        new EnvBuilder().withName(EnvConverter.convertName(e.getName())).withValue(e.getValue())
                                .withSecret(e.getSecret()).withConfigmap(e.getConfigMap()).withField(e.getField())
                                .withPrefix(e.getPrefix())
                                .build()))));

        config.containerName().ifPresent(containerName -> result
                .add(new DecoratorBuildItem(KUBERNETES, new ChangeContainerNameDecorator(containerName))));

        // Service handling
        result.add(new DecoratorBuildItem(KUBERNETES, new ApplyServiceTypeDecorator(name, config.serviceType().name())));
        if ((config.serviceType() == ServiceType.NodePort)) {
            List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports().entrySet().stream()
                    .filter(e -> e.getValue().nodePort().isPresent())
                    .toList();
            if (!nodeConfigPorts.isEmpty()) {
                for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                    result.add(new DecoratorBuildItem(KUBERNETES,
                            new AddNodePortDecorator(name, entry.getValue().nodePort().getAsInt(), entry.getKey())));
                }
            } else if (config.nodePort().isPresent()) {
                result.add(new DecoratorBuildItem(KUBERNETES,
                        new AddNodePortDecorator(name, config.nodePort().getAsInt(), config.ingress().targetPort())));
            }
        }

        // Probe port handling
        result.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, KUBERNETES, LIVENESS_PROBE, config.livenessProbe(),
                        portName,
                        ports,
                        config.ports()));
        result.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, KUBERNETES, READINESS_PROBE, config.readinessProbe(),
                        portName,
                        ports,
                        config.ports()));
        result.add(
                KubernetesCommonHelper.createProbeHttpPortDecorator(name, KUBERNETES, STARTUP_PROBE, config.startupProbe(),
                        portName,
                        ports,
                        config.ports()));

        // Handle remote debug configuration
        if (config.remoteDebug().enabled()) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                    config.remoteDebug().buildJavaToolOptionsEnv())));
        }

        // Handle init Containers and Jobs
        result.addAll(KubernetesCommonHelper.createInitContainerDecorators(KUBERNETES, name, initContainers, result));
        result.addAll(KubernetesCommonHelper.createInitJobDecorators(KUBERNETES, name, jobs, result));

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        if (managementPortIsEnabled()
                && (config.ingress() == null
                        || !config.ingress().expose()
                        || !config.ingress().targetPort().equals(MANAGEMENT_PORT_NAME))) {
            result.add(new DecoratorBuildItem(KUBERNETES, new RemovePortFromServiceDecorator(name, MANAGEMENT_PORT_NAME)));
        }

        // Handle deployment strategy
        if (config.strategy() != DeploymentStrategy.None) {
            result.add(new DecoratorBuildItem(KUBERNETES,
                    new ApplyDeploymentStrategyDecorator(name, config.strategy(), new RollingUpdateBuilder()
                            .withMaxSurge(config.rollingUpdate().maxSurge())
                            .withMaxUnavailable(config.rollingUpdate().maxUnavailable())
                            .build())));
        }
        printMessageAboutPortsThatCantChange(KUBERNETES, ports, config);
        return result;
    }

    @BuildStep
    void externalizeInitTasks(
            ApplicationInfoBuildItem applicationInfo,
            KubernetesConfig config,
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
        if (config.externalizeInit()) {
            InitTaskProcessor.process(KUBERNETES, name, image, initTasks, config.initTaskDefaults(), config.initTasks(),
                    jobs, initContainers, env, roles, roleBindings, serviceAccount, decorators);
        }
    }
}
