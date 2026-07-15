package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_HTTP_PORT;
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

import org.jboss.logging.Logger;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.IngressRuleBuilder;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.AddAnnotationDecorator;
import io.dekorate.kubernetes.decorator.AddIngressRuleDecorator;
import io.dekorate.kubernetes.decorator.AddServiceResourceDecorator;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.kubernetes.spi.CustomProjectRootBuildItem;
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
    private static final Logger log = Logger.getLogger(BaseVanillaKubernetesProcessor.class);

    @Override
    protected AddPortToKubernetesConfig portConfigurator(Port port) {
        return new AddPortToKubernetesConfig(port);
    }

    @Override
    protected Optional<Port> optionalPort(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected DecoratorsContext decorators(
            ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            Capabilities capabilities, PackageConfig packageConfig,
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
        final var context = commonDecorators(applicationInfo, outputTarget, capabilities, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, annotations, labels, envs, image, command,
                ports, livenessPath, readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings,
                clusterRoleBindings, customProjectRoot, targets);
        if (context.done()) {
            return context;
        }

        final var config = config();
        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        if (managementPortIsEnabled()
                && !isExposingManagementPort(config)) {
            context.add(new RemovePortFromServiceDecorator(context.name(), MANAGEMENT_PORT_NAME));
        }

        // Probe port handling
        probes(context, ports, portName);

        // Handle init Containers
        initTasks(context, initContainers, jobs);

        // Service handling
        service(context, config);

        ingress(context, ports, config);
        gateway(context, ports, config);

        return context;
    }

    private static boolean isExposingManagementPort(KubernetesConfig config) {
        return isIngressExposingManagement(config) || isGatewayExposingManagement(config);
    }

    private static boolean isIngressExposingManagement(KubernetesConfig config) {
        return config.ingress() != null
                && config.ingress().expose()
                && MANAGEMENT_PORT_NAME.equals(config.ingress().targetPort());
    }

    private static boolean isGatewayExposingManagement(KubernetesConfig config) {
        return config.gateway() != null
                && config.gateway().expose()
                && MANAGEMENT_PORT_NAME.equals(config.gateway().targetPort());
    }

    protected void ingress(DecoratorsContext context, List<KubernetesPortBuildItem> ports, KubernetesConfig config) {
        if (config.ingress() == null) {
            return;
        }

        for (Map.Entry<String, String> annotation : config.ingress().annotations().entrySet()) {
            context.add(new AddAnnotationDecorator(context.name(), annotation.getKey(), annotation.getValue(), INGRESS));
        }

        for (IngressConfig.IngressRuleConfig rule : config.ingress().rules().values()) {
            context.add(new AddIngressRuleDecorator(context.name(), optionalPort(ports),
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

    protected void gateway(DecoratorsContext context, List<KubernetesPortBuildItem> ports, KubernetesConfig config) {
        if (config.gateway() == null || !config.gateway().expose()) {
            return;
        }

        GatewayConfig gateway = config.gateway();

        if (config.ingress() != null && config.ingress().expose()) {
            log.warn("Both quarkus.kubernetes.ingress.expose and quarkus.kubernetes.gateway.expose are true; "
                    + "generating both Ingress and Gateway API resources.");
        }

        boolean hasParentRefs = gateway.parentRefs() != null && !gateway.parentRefs().isEmpty();
        if (!gateway.generateGateway() && !hasParentRefs) {
            throw new IllegalArgumentException(
                    "quarkus.kubernetes.gateway.expose=true requires either "
                            + "quarkus.kubernetes.gateway.parent-refs.<id>.name or "
                            + "quarkus.kubernetes.gateway.generate-gateway=true");
        }

        if (gateway.generateGateway() && gateway.gatewayClassName().isEmpty()) {
            throw new IllegalArgumentException(
                    "quarkus.kubernetes.gateway.generate-gateway=true requires "
                            + "quarkus.kubernetes.gateway.gateway-class-name");
        }

        if (gateway.generateGateway() && hasParentRefs) {
            log.warn("Both quarkus.kubernetes.gateway.generate-gateway and "
                    + "quarkus.kubernetes.gateway.parent-refs are set; "
                    + "HTTPRoute will use parent-refs and the generated Gateway may be unused.");
        }

        Optional<Port> port = KubernetesCommonHelper.getPort(ports, config, gateway.targetPort());
        int backendPort = port.map(AddServiceResourceDecorator::calculateHostPort).orElse(DEFAULT_HTTP_PORT);

        String path = gateway.path();
        PortConfig portConfig = config.ports().get(gateway.targetPort());
        if (portConfig != null && portConfig.path().isPresent()) {
            path = portConfig.path().get();
        }

        List<String> hostnames = AddHttpRouteResourceDecorator.combineHostnames(gateway.host(), gateway.hosts());
        String resourceName = context.name();

        if (gateway.generateGateway()) {
            warnUnsupportedTlsListeners(gateway.listeners());
            context.add(new AddGatewayResourceDecorator(
                    resourceName,
                    gateway.gatewayClassName().get(),
                    gateway.listeners(),
                    gateway.annotations(),
                    hostnames.stream().findFirst()));
        }

        List<AddHttpRouteResourceDecorator.Rule> extraRules = new ArrayList<>();
        if (gateway.rules() != null) {
            for (GatewayConfig.GatewayRuleConfig rule : gateway.rules().values()) {
                int rulePort = resolveGatewayBackendPort(ports, config, resourceName, rule, backendPort);
                extraRules.add(new AddHttpRouteResourceDecorator.Rule(
                        rule.path(),
                        rule.pathType(),
                        rule.serviceName().orElse(resourceName),
                        rulePort));
            }
        }

        context.add(new AddHttpRouteResourceDecorator(
                resourceName,
                hostnames,
                path,
                gateway.pathType(),
                resourceName,
                backendPort,
                gateway.parentRefs(),
                extraRules,
                gateway.annotations(),
                gateway.generateGateway(),
                resourceName));
    }

    private static void warnUnsupportedTlsListeners(Map<String, GatewayConfig.ListenerConfig> listeners) {
        if (listeners == null) {
            return;
        }
        for (GatewayConfig.ListenerConfig listener : listeners.values()) {
            String protocol = listener.protocol();
            if ("HTTPS".equalsIgnoreCase(protocol) || "TLS".equalsIgnoreCase(protocol)) {
                log.warnf(
                        "Gateway listener '%s' uses protocol %s, but quarkus.kubernetes.gateway does not generate "
                                + "listener TLS certificateRefs yet; the Gateway may be invalid until TLS is configured "
                                + "out-of-band.",
                        listener.name(), protocol);
            }
        }
    }

    private static int resolveGatewayBackendPort(List<KubernetesPortBuildItem> ports, KubernetesConfig config,
            String applicationServiceName, GatewayConfig.GatewayRuleConfig rule, int defaultPort) {
        if (rule.servicePortNumber().isPresent()) {
            return rule.servicePortNumber().get();
        }

        Optional<String> customService = rule.serviceName()
                .filter(name -> !name.equals(applicationServiceName));
        if (customService.isPresent()) {
            throw new IllegalArgumentException(
                    "quarkus.kubernetes.gateway.rules.<id>.service-port-number is required when "
                            + "service-name points to a different service ('" + customService.get() + "'). "
                            + "Named ports from the current application cannot be resolved for another service.");
        }

        String portName = rule.servicePortName().orElse(config.gateway().targetPort());
        return KubernetesCommonHelper.getPort(ports, config, portName)
                .map(AddServiceResourceDecorator::calculateHostPort)
                .orElse(defaultPort);
    }

    protected void service(DecoratorsContext context, KubernetesConfig config) {
        context.add(new ApplyServiceTypeDecorator(context.name(), ServiceType.NodePort.name()));
        List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.ports().entrySet().stream()
                .filter(e -> e.getValue().nodePort().isPresent())
                .toList();
        if (!nodeConfigPorts.isEmpty()) {
            for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                context.add(new AddNodePortDecorator(context.name(), entry.getValue().nodePort().getAsInt(), entry.getKey()));
            }
        } else {
            context.add(new AddNodePortDecorator(context.name(),
                    config.nodePort().orElseGet(
                            () -> getStablePortNumberInRange(context.name(), MIN_NODE_PORT_VALUE, MAX_NODE_PORT_VALUE)),
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
