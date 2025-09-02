package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.INGRESS;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.LIVENESS_PROBE;
import static io.quarkus.kubernetes.deployment.Constants.MAX_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MAX_PORT_NUMBER;
import static io.quarkus.kubernetes.deployment.Constants.MIN_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MIN_PORT_NUMBER;
import static io.quarkus.kubernetes.deployment.Constants.READINESS_PROBE;
import static io.quarkus.kubernetes.deployment.Constants.STARTUP_PROBE;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.MANAGEMENT_PORT_NAME;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.managementPortIsEnabled;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.config.IngressRuleBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.project.Project;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesClusterRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
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
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.Targetable;

public class DevClusterHelper {

    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static List<DecoratorBuildItem> createDecorators(String clusterKind,
            String deploymentTarget,
            ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            KubernetesConfig config,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            List<KubernetesNamespaceBuildItem> namespaces,
            List<KubernetesInitContainerBuildItem> initContainers,
            List<KubernetesJobBuildItem> jobs,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            List<KubernetesEnvBuildItem> envs,
            @Deprecated Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            List<KubernetesPortBuildItem> ports,
            Optional<KubernetesProbePortNameBuildItem> portName,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesClusterRoleBuildItem> clusterRoles,
            List<KubernetesEffectiveServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot) {

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        final var namespace = Targetable.filteredByTarget(namespaces, deploymentTarget, true)
                .findFirst();

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        Optional<Port> port = KubernetesCommonHelper.getPort(ports, config);

        List<DecoratorBuildItem> result = new ArrayList<>(
                KubernetesCommonHelper.createDecorators(project, clusterKind, name, namespace, config,
                        metricsConfiguration, kubernetesClientConfiguration,
                        annotations, labels, image, command,
                        port, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                        clusterRoleBindings));

        image.ifPresent(
                i -> result.add(new DecoratorBuildItem(clusterKind, new ApplyContainerImageDecorator(name, i.getImage()))));

        var stream = Stream.concat(config.convertToBuildItems().stream(), Targetable.filteredByTarget(envs, KUBERNETES));
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

        result.add(new DecoratorBuildItem(clusterKind, new ApplyImagePullPolicyDecorator(name, "IfNotPresent")));

        //Service handling
        result.add(new DecoratorBuildItem(clusterKind, new ApplyServiceTypeDecorator(name, ServiceType.NodePort.name())));
        List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports().entrySet().stream()
                .filter(e -> e.getValue().nodePort().isPresent())
                .toList();
        if (!nodeConfigPorts.isEmpty()) {
            for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                result.add(new DecoratorBuildItem(KUBERNETES,
                        new AddNodePortDecorator(name, entry.getValue().nodePort().getAsInt(), entry.getKey())));
            }
        } else {
            result.add(new DecoratorBuildItem(clusterKind,
                    new AddNodePortDecorator(name,
                            config.nodePort().orElseGet(
                                    () -> getStablePortNumberInRange(name, MIN_NODE_PORT_VALUE, MAX_NODE_PORT_VALUE)),
                            config.ingress().targetPort())));
        }

        //Probe port handling
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

        // Handle init Containers
        result.addAll(KubernetesCommonHelper.createInitContainerDecorators(clusterKind, name, initContainers, result));
        result.addAll(KubernetesCommonHelper.createInitJobDecorators(clusterKind, name, jobs, result));

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        if (managementPortIsEnabled()
                && (config.ingress() == null
                        || !config.ingress().expose()
                        || !config.ingress().targetPort().equals(MANAGEMENT_PORT_NAME))) {
            result.add(new DecoratorBuildItem(clusterKind, new RemovePortFromServiceDecorator(name, MANAGEMENT_PORT_NAME)));
        }
        return result;
    }

    /**
     * Given a string, generate a port number within the supplied range
     * The output is always the same (between {@code min} and {@code max})
     * given the same input and it's useful when we need to generate a port number
     * which needs to stay the same but we don't care about the exact value
     */
    private static int getStablePortNumberInRange(String input, int min, int max) {
        if (min < MIN_PORT_NUMBER || max > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException(
                    String.format("Port number range must be within [%d-%d]", MIN_PORT_NUMBER, MAX_PORT_NUMBER));
        }

        try {
            byte[] hash = MessageDigest.getInstance(DEFAULT_HASH_ALGORITHM).digest(input.getBytes(StandardCharsets.UTF_8));
            return min + new BigInteger(hash).mod(BigInteger.valueOf(max - min)).intValue();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate stable port number from input string: '" + input + "'", e);
        }
    }
}
