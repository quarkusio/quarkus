
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.INGRESS;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.KubernetesCommonHelper.printMessageAboutPortsThatCantChange;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.VANILLA_KUBERNETES_PRIORITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.DeploymentStrategy;
import io.dekorate.kubernetes.config.IngressBuilder;
import io.dekorate.kubernetes.config.IngressRuleBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.config.RollingUpdateBuilder;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.AddIngressTlsDecorator;
import io.dekorate.kubernetes.decorator.ApplyDeploymentStrategyDecorator;
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

public class VanillaKubernetesProcessor extends BaseProcessor<KubernetesConfig> {

    public VanillaKubernetesProcessor() {
        super(KUBERNETES, VANILLA_KUBERNETES_PRIORITY);
    }

    @BuildStep
    public void checkVanillaKubernetes(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities,
            KubernetesConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        DeploymentResourceKind deploymentResourceKind = config.getDeploymentResourceKind(capabilities);

        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getConfiguredDeploymentTargets();
        if (userSpecifiedDeploymentTargets.isEmpty() || userSpecifiedDeploymentTargets.contains(flavor)) {
            // when nothing was selected by the user, we enable vanilla Kubernetes by default
            deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(flavor,
                    deploymentResourceKind.getKind(), deploymentResourceKind.getGroup(), deploymentResourceKind.getVersion(),
                    priority, true, config.deployStrategy));

            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(new KubernetesResourceMetadataBuildItem(flavor, deploymentResourceKind.getGroup(),
                    deploymentResourceKind.getVersion(), deploymentResourceKind.getKind(), name));

        } else {
            deploymentTargets
                    .produce(new KubernetesDeploymentTargetBuildItem(flavor, deploymentResourceKind.getKind(),
                            deploymentResourceKind.getGroup(),
                            deploymentResourceKind.getVersion(), priority, false, config.deployStrategy));
        }
    }

    @BuildStep
    public void createAnnotations(KubernetesConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        doCreateAnnotations(config, annotations);
    }

    @BuildStep
    public void createLabels(KubernetesConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        doCreateLabels(config, labels, imageLabels);
        labels.produce(new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", flavor));
    }

    @BuildStep
    public void createNamespace(KubernetesConfig config, BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        doCreateNamespace(config, namespace);
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(KubernetesConfig config, List<KubernetesPortBuildItem> ports) {
        List<ConfiguratorBuildItem> result = new ArrayList<>();
        KubernetesCommonHelper.combinePorts(ports, config).values().forEach(value -> {
            result.add(new ConfiguratorBuildItem(new AddPortToKubernetesConfig(value)));
        });
        if (config.ingress != null) {
            result.add(new ConfiguratorBuildItem(new ApplyKubernetesIngressConfigurator((config.ingress))));
        }

        // Handle remote debug configuration for container ports
        if (config.remoteDebug.enabled) {
            result.add(new ConfiguratorBuildItem(new AddPortToKubernetesConfig(config.remoteDebug.buildDebugPort())));
        }

        return result;
    }

    @Override
    protected Optional<Port> createPort(KubernetesConfig config, List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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
            List<KubernetesServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings, Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets,
            BuildProducer<KubernetesEffectiveServiceAccountBuildItem> serviceAccountProducer) {

        final var manifestInfo = doCreateDecorators(applicationInfo, outputTarget, config, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, annotations, labels, envs, image, command, ports, livenessPath,
                readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings, customProjectRoot, targets);

        if (manifestInfo.skipFurtherProcessing()) {
            return manifestInfo.getDecoratorsAndProduceServiceAccountBuildItem(serviceAccountProducer);
        }

        containerImageDecorators(config, manifestInfo, image);

        replicasDecorators(config, manifestInfo);

        final var name = manifestInfo.getDefaultName();

        DeploymentResourceKind deploymentKind = config.getDeploymentResourceKind(capabilities);
        if (deploymentKind != DeploymentResourceKind.Deployment) {
            manifestInfo.add(new DecoratorBuildItem(flavor, new RemoveDeploymentResourceDecorator(name)));
        }
        switch (deploymentKind) {
            case StatefulSet ->
                manifestInfo.add(new DecoratorBuildItem(flavor, new AddStatefulSetResourceDecorator(name, config)));
            case Job -> manifestInfo.add(new DecoratorBuildItem(flavor, new AddJobResourceDecorator(name, config.job)));
            case CronJob ->
                manifestInfo.add(new DecoratorBuildItem(flavor, new AddCronJobResourceDecorator(name, config.cronJob)));
        }

        if (config.ingress != null) {
            if (config.ingress.tls != null) {
                for (Map.Entry<String, IngressTlsConfig> tlsConfigEntry : config.ingress.tls.entrySet()) {
                    if (tlsConfigEntry.getValue().enabled) {
                        String[] tlsHosts = tlsConfigEntry.getValue().hosts
                                .map(l -> l.toArray(new String[0]))
                                .orElse(null);
                        manifestInfo.add(new DecoratorBuildItem(flavor,
                                new AddIngressTlsDecorator(name, new IngressBuilder()
                                        .withTlsSecretName(tlsConfigEntry.getKey())
                                        .withTlsHosts(tlsHosts)
                                        .build())));
                    }
                }

            }
            for (Map.Entry<String, String> annotation : config.ingress.annotations.entrySet()) {
                manifestInfo.add(new DecoratorBuildItem(flavor,
                        new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), INGRESS)));
            }

            for (IngressRuleConfig rule : config.ingress.rules.values()) {
                manifestInfo.add(new DecoratorBuildItem(flavor, new AddIngressRuleDecorator(name, manifestInfo.getPort(),
                        new IngressRuleBuilder()
                                .withHost(rule.host)
                                .withPath(rule.path)
                                .withPathType(rule.pathType)
                                .withServiceName(rule.serviceName.orElse(null))
                                .withServicePortName(rule.servicePortName.orElse(null))
                                .withServicePortNumber(rule.servicePortNumber.orElse(-1))
                                .build())));
            }
        }

        manifestInfo.add(new DecoratorBuildItem(flavor, new AddSelectorToDeploymentDecorator(name)));

        // Service handling
        serviceDecorators(config, manifestInfo, name);

        // Probe port handling
        probePortDecorators(config, manifestInfo, portName, ports);

        // Handle remote debug configuration
        remoteDebugDecorators(config, manifestInfo);

        // Handle init Containers and Jobs
        initContainersAndJobsDecorators(config, manifestInfo, initContainers, jobs);

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        managementPortDecorators(config, manifestInfo);

        // Handle deployment strategy
        if (config.strategy != DeploymentStrategy.None) {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new ApplyDeploymentStrategyDecorator(name, config.strategy, new RollingUpdateBuilder()
                            .withMaxSurge(config.rollingUpdate.maxSurge)
                            .withMaxUnavailable(config.rollingUpdate.maxUnavailable)
                            .build())));
        }

        printMessageAboutPortsThatCantChange(flavor, ports, config);
        return manifestInfo.getDecoratorsAndProduceServiceAccountBuildItem(serviceAccountProducer);
    }

    private void serviceDecorators(KubernetesConfig config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo,
            String name) {
        manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));
        if ((config.getServiceType() == ServiceType.NodePort)) {
            List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports.entrySet().stream()
                    .filter(e -> e.getValue().nodePort.isPresent())
                    .toList();
            if (!nodeConfigPorts.isEmpty()) {
                for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                    manifestInfo.add(new DecoratorBuildItem(flavor,
                            new AddNodePortDecorator(name, entry.getValue().nodePort.getAsInt(), entry.getKey())));
                }
            } else if (config.nodePort.isPresent()) {
                manifestInfo.add(new DecoratorBuildItem(flavor,
                        new AddNodePortDecorator(name, config.nodePort.getAsInt(), config.ingress.targetPort)));
            }
        }
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
        doExternalizeInitTasks(applicationInfo, config, image, initTasks, jobs, initContainers, env, roles, roleBindings,
                serviceAccount, decorators);
    }
}
