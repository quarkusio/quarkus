package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.INGRESS;
import static io.quarkus.kubernetes.deployment.Constants.MAX_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MAX_PORT_NUMBER;
import static io.quarkus.kubernetes.deployment.Constants.MIN_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MIN_PORT_NUMBER;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.MANAGEMENT_PORT_NAME;
import static io.quarkus.kubernetes.deployment.KubernetesConfigUtil.managementPortIsEnabled;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.IngressRuleBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
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
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public abstract class BaseVanillaKubernetesProcessor extends BaseKubeProcessor<AddPortToKubernetesConfig, KubernetesConfig> {
    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    @Override
    protected AddPortToKubernetesConfig portConfigurator(Port port) {
        return new AddPortToKubernetesConfig(port);
    }

    @Override
    protected Optional<Port> optionalPort(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public List<DecoratorBuildItem> createDecorators(
            ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Optional<KubernetesClientCapabilityBuildItem> kubernetesClientConfiguration,
            List<KubernetesNamespaceBuildItem> namespaces,
            List<KubernetesInitContainerBuildItem> initContainers,
            List<KubernetesJobBuildItem> jobs,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            List<KubernetesEnvBuildItem> envs,
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
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            List<KubernetesDeploymentTargetBuildItem> targets) {
        if (isDeploymentTargetDisabled(targets)) {
            return new ArrayList<>();
        }

        final var config = config();
        final var clusterKind = deploymentTarget();
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);

        final var result = commonDecorators(applicationInfo, outputTarget, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, annotations, labels, envs, image, command,
                ports, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                clusterRoleBindings, customProjectRoot);

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        if (managementPortIsEnabled()
                && (config.ingress() == null
                        || !config.ingress().expose()
                        || !config.ingress().targetPort().equals(MANAGEMENT_PORT_NAME))) {
            addDecorator(result, new RemovePortFromServiceDecorator(name, MANAGEMENT_PORT_NAME));
        }

        // Probe port handling
        probes(ports, portName, result, name);

        // Handle init Containers
        initTasks(initContainers, jobs, result, name);

        // Service handling
        service(result, clusterKind, name, config);

        ingress(ports, config, result, clusterKind, name);

        return result;
    }

    protected void ingress(List<KubernetesPortBuildItem> ports, KubernetesConfig config, List<DecoratorBuildItem> result,
            String clusterKind, String name) {
        if (config.ingress() == null) {
            return;
        }

        for (Map.Entry<String, String> annotation : config.ingress().annotations().entrySet()) {
            addDecorator(result, new AddAnnotationDecorator(name, annotation.getKey(), annotation.getValue(), INGRESS));
        }

        for (IngressConfig.IngressRuleConfig rule : config.ingress().rules().values()) {
            addDecorator(result, new AddIngressRuleDecorator(name, optionalPort(ports),
                    new IngressRuleBuilder()
                            .withHost(rule.host())
                            .withPath(rule.path())
                            .withPathType(rule.pathType())
                            .withServiceName(rule.serviceName().orElse(null))
                            .withServicePortName(rule.servicePortName().orElse(null))
                            .withServicePortNumber(rule.servicePortNumber().orElse(-1))
                            .build()));
        }
    }

    protected void service(List<DecoratorBuildItem> result, String clusterKind, String name, KubernetesConfig config) {
        addDecorator(result, new ApplyServiceTypeDecorator(name, ServiceType.NodePort.name()));
        List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports().entrySet().stream()
                .filter(e -> e.getValue().nodePort().isPresent())
                .toList();
        if (!nodeConfigPorts.isEmpty()) {
            for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                addDecorator(result, new AddNodePortDecorator(name, entry.getValue().nodePort().getAsInt(), entry.getKey()));
            }
        } else {
            addDecorator(result, new AddNodePortDecorator(name,
                    config.nodePort().orElseGet(
                            () -> getStablePortNumberInRange(name, MIN_NODE_PORT_VALUE, MAX_NODE_PORT_VALUE)),
                    config.ingress().targetPort()));
        }
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
