
package io.quarkus.kubernetes.deployment;

import static io.dekorate.kubernetes.decorator.AddServiceResourceDecorator.distinct;
import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.INGRESS;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.VANILLA_KUBERNETES_PRIORITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.IngressBuilder;
import io.dekorate.kubernetes.config.IngressRuleBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.AddIngressTlsDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
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
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public class VanillaKubernetesProcessor {

    @BuildStep
    public void checkVanillaKubernetes(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities,
            KubernetesConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        String kind = config.getDeploymentResourceKind(capabilities).kind;

        List<String> userSpecifiedDeploymentTargets = KubernetesConfigUtil.getUserSpecifiedDeploymentTargets();
        if (userSpecifiedDeploymentTargets.isEmpty() || userSpecifiedDeploymentTargets.contains(KUBERNETES)) {
            // when nothing was selected by the user, we enable vanilla Kubernetes by
            // default
            deploymentTargets
                    .produce(
                            new KubernetesDeploymentTargetBuildItem(KUBERNETES, kind, DEPLOYMENT_GROUP,
                                    DEPLOYMENT_VERSION,
                                    VANILLA_KUBERNETES_PRIORITY, true));

            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(new KubernetesResourceMetadataBuildItem(KUBERNETES, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION,
                    kind, name));

        } else {
            deploymentTargets
                    .produce(new KubernetesDeploymentTargetBuildItem(KUBERNETES, kind, DEPLOYMENT_GROUP,
                            DEPLOYMENT_VERSION,
                            VANILLA_KUBERNETES_PRIORITY, false));
        }
    }

    @BuildStep
    public void createAnnotations(KubernetesConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.annotations.forEach((k, v) -> {
            annotations.produce(new KubernetesAnnotationBuildItem(k, v, KUBERNETES));
        });
    }

    @BuildStep
    public void createLabels(KubernetesConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config.labels.forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, KUBERNETES));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
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

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget, Capabilities capabilities, KubernetesConfig config, PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration, List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels, List<KubernetesEnvBuildItem> envs,
            Optional<ContainerImageInfoBuildItem> image, Optional<KubernetesCommandBuildItem> command,
            List<KubernetesPortBuildItem> ports, Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath, List<KubernetesRoleBuildItem> roles,
            List<KubernetesRoleBindingBuildItem> roleBindings, Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets) {

        final List<DecoratorBuildItem> result = new ArrayList<>();
        if (!targets.stream().filter(KubernetesDeploymentTargetBuildItem::isEnabled)
                .anyMatch(t -> KUBERNETES.equals(t.getName()))) {
            return result;
        }
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        result.addAll(KubernetesCommonHelper.createDecorators(project, KUBERNETES, name, config, metricsConfiguration,
                annotations, labels, command, ports, livenessPath, readinessPath, roles, roleBindings));

        KubernetesConfig.DeploymentResourceKind deploymentKind = config.getDeploymentResourceKind(capabilities);
        if (deploymentKind != KubernetesConfig.DeploymentResourceKind.Deployment) {
            result.add(new DecoratorBuildItem(KUBERNETES, new RemoveDeploymentResourceDecorator(name)));
        }

        if (deploymentKind == KubernetesConfig.DeploymentResourceKind.StatefulSet) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddStatefulSetResourceDecorator(name, config)));
        } else if (deploymentKind == KubernetesConfig.DeploymentResourceKind.Job) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddJobResourceDecorator(name, config.job)));
        } else if (deploymentKind == KubernetesConfig.DeploymentResourceKind.CronJob) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddCronJobResourceDecorator(name, config.cronJob)));
        }

        if (config.ingress != null) {
            if (config.ingress.tls != null) {
                for (Map.Entry<String, IngressTlsConfig> tlsConfigEntry : config.ingress.tls.entrySet()) {
                    if (tlsConfigEntry.getValue().enabled) {
                        String[] tlsHosts = tlsConfigEntry.getValue().hosts
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
            for (Map.Entry<String, String> annotation : config.ingress.annotations.entrySet()) {
                result.add(new DecoratorBuildItem(KUBERNETES,
                        new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), INGRESS)));
            }

            Optional<Port> defaultHostPort = KubernetesCommonHelper.combinePorts(ports, config).values().stream()
                    .filter(distinct(p -> p.getName()))
                    .findFirst();

            for (IngressRuleConfig rule : config.ingress.rules.values()) {
                result.add(new DecoratorBuildItem(KUBERNETES, new AddIngressRuleDecorator(name, defaultHostPort,
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

        if (config.getReplicas() != 1) {
            // This only affects Deployment
            result.add(new DecoratorBuildItem(KUBERNETES, new ApplyReplicasToDeploymentDecorator(name, config.getReplicas())));
            // This only affects StatefulSet
            result.add(new DecoratorBuildItem(KUBERNETES, new ApplyReplicasToStatefulSetDecorator(name, config.getReplicas())));
        }

        image.ifPresent(i -> {
            result.add(new DecoratorBuildItem(KUBERNETES, new ApplyContainerImageDecorator(name, i.getImage())));
        });

        result.add(new DecoratorBuildItem(KUBERNETES, new ApplyImagePullPolicyDecorator(name, config.getImagePullPolicy())));
        result.add(new DecoratorBuildItem(KUBERNETES, new AddSelectorToDeploymentDecorator(name)));

        Stream.concat(config.convertToBuildItems().stream(),
                envs.stream().filter(e -> e.getTarget() == null || KUBERNETES.equals(e.getTarget()))).forEach(e -> {
                    result.add(new DecoratorBuildItem(KUBERNETES,
                            new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                                    new EnvBuilder().withName(EnvConverter.convertName(e.getName())).withValue(e.getValue())
                                            .withSecret(e.getSecret()).withConfigmap(e.getConfigMap()).withField(e.getField())
                                            .build())));
                });

        config.getContainerName().ifPresent(containerName -> result
                .add(new DecoratorBuildItem(KUBERNETES, new ChangeContainerNameDecorator(containerName))));

        // Service handling
        result.add(new DecoratorBuildItem(KUBERNETES, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));
        if ((config.getServiceType() == ServiceType.NodePort)) {
            List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports.entrySet().stream()
                    .filter(e -> e.getValue().nodePort.isPresent())
                    .collect(Collectors.toList());
            if (!nodeConfigPorts.isEmpty()) {
                for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                    result.add(new DecoratorBuildItem(KUBERNETES,
                            new AddNodePortDecorator(name, entry.getValue().nodePort.getAsInt(), Optional.of(entry.getKey()))));
                }
            } else if (config.nodePort.isPresent()) {
                result.add(new DecoratorBuildItem(KUBERNETES, new AddNodePortDecorator(name, config.nodePort.getAsInt())));
            }
        }

        // Probe port handling
        Integer port = ports.stream().filter(p -> HTTP_PORT.equals(p.getName())).map(KubernetesPortBuildItem::getPort)
                .findFirst().orElse(DEFAULT_HTTP_PORT);
        result.add(new DecoratorBuildItem(KUBERNETES, new ApplyHttpGetActionPortDecorator(name, name, port)));

        // Handle remote debug configuration
        if (config.remoteDebug.enabled) {
            result.add(new DecoratorBuildItem(KUBERNETES, new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                    config.remoteDebug.buildJavaToolOptionsEnv())));
        }

        return result;
    }

}
