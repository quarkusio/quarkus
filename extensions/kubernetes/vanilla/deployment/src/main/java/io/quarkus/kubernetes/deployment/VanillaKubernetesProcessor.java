package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.KubernetesCommonHelper.printMessageAboutPortsThatCantChange;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.VANILLA_KUBERNETES_PRIORITY;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.DeploymentStrategy;
import io.dekorate.kubernetes.config.IngressBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.RollingUpdateBuilder;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddIngressTlsDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyDeploymentStrategyDecorator;
import io.dekorate.kubernetes.decorator.ApplyReplicasToDeploymentDecorator;
import io.dekorate.kubernetes.decorator.ApplyReplicasToStatefulSetDecorator;
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

public class VanillaKubernetesProcessor extends BaseVanillaKubernetesProcessor {
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
    protected Optional<Port> optionalPort(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config());
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
        final var context = super.decorators(applicationInfo, outputTarget, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, initContainers, jobs, annotations, labels, envs, image, command,
                ports, portName, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                clusterRoleBindings, customProjectRoot, targets);
        if (context.done()) {
            return context.decorators();
        }
        final var config = config();
        final var name = context.name();

        deploymentKindDecorators(context, capabilities);

        if (config.replicas() != 1) {
            // This only affects Deployment
            context.add(new ApplyReplicasToDeploymentDecorator(name, config.replicas()));
            // This only affects StatefulSet
            context.add(new ApplyReplicasToStatefulSetDecorator(name, config.replicas()));
        }

        context.add(new AddSelectorToDeploymentDecorator(name));

        config.containerName().ifPresent(containerName -> context.add(new ChangeContainerNameDecorator(containerName)));

        // Handle remote debug configuration
        if (config.remoteDebug().enabled()) {
            context.add(new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                    config.remoteDebug().buildJavaToolOptionsEnv()));
        }

        // Handle deployment strategy
        if (config.strategy() != DeploymentStrategy.None) {
            context.add(new ApplyDeploymentStrategyDecorator(name, config.strategy(),
                    new RollingUpdateBuilder()
                            .withMaxSurge(config.rollingUpdate().maxSurge())
                            .withMaxUnavailable(config.rollingUpdate().maxUnavailable())
                            .build()));
        }
        printMessageAboutPortsThatCantChange(context.target(), ports, config);
        return context.decorators();
    }

    @Override
    protected void service(DecoratorsContext context, KubernetesConfig config) {
        final var name = context.name();
        context.add(new ApplyServiceTypeDecorator(name, config.serviceType().name()));
        if ((config.serviceType() == ServiceType.NodePort)) {
            List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports().entrySet().stream()
                    .filter(e -> e.getValue().nodePort().isPresent())
                    .toList();
            if (!nodeConfigPorts.isEmpty()) {
                for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                    context.add(new AddNodePortDecorator(name, entry.getValue().nodePort().getAsInt(), entry.getKey()));
                }
            } else if (config.nodePort().isPresent()) {
                context.add(new AddNodePortDecorator(name, config.nodePort().getAsInt(), config.ingress().targetPort()));
            }
        }
    }

    @Override
    protected void ingress(DecoratorsContext context, List<KubernetesPortBuildItem> ports, KubernetesConfig config) {
        super.ingress(context, ports, config);

        if (config.ingress() != null && config.ingress().tls() != null) {
            for (Map.Entry<String, IngressConfig.IngressTlsConfig> tlsConfigEntry : config.ingress().tls().entrySet()) {
                if (tlsConfigEntry.getValue().enabled()) {
                    String[] tlsHosts = tlsConfigEntry.getValue().hosts()
                            .map(l -> l.toArray(new String[0]))
                            .orElse(null);
                    context.add(new AddIngressTlsDecorator(context.name(), new IngressBuilder()
                            .withTlsSecretName(tlsConfigEntry.getKey())
                            .withTlsHosts(tlsHosts)
                            .build()));
                }
            }
        }
    }

    private void deploymentKindDecorators(DecoratorsContext context, Capabilities capabilities) {
        final var deploymentKind = deploymentResourceKind(capabilities);
        final var name = context.name();
        if (deploymentKind != DeploymentResourceKind.Deployment) {
            context.add(new RemoveDeploymentResourceDecorator(name));
        }
        if (deploymentKind == DeploymentResourceKind.StatefulSet) {
            context.add(new AddStatefulSetResourceDecorator(name, config));
        } else if (deploymentKind == DeploymentResourceKind.Job) {
            context.add(new AddJobResourceDecorator(name, config.job()));
        } else if (deploymentKind == DeploymentResourceKind.CronJob) {
            context.add(new AddCronJobResourceDecorator(name, config.cronJob()));
        }
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
