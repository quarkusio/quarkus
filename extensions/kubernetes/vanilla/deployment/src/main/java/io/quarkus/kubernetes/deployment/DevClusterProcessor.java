package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.Port;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
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

public class DevClusterProcessor extends BaseProcessor<KubernetesConfig> {
    private static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    public DevClusterProcessor(String flavor, int priority, String deploymentTarget) {
        super(flavor, priority, deploymentTarget);
    }

    private void serviceDecorators(KubernetesConfig config, KubernetesCommonHelper.ManifestGenerationInfo manifestInfo) {
        final var name = manifestInfo.getDefaultName();
        manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyServiceTypeDecorator(name, ServiceType.NodePort.name())));
        List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.getPorts().entrySet().stream()
                .filter(e -> e.getValue().nodePort.isPresent())
                .toList();
        if (!nodeConfigPorts.isEmpty()) {
            for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                manifestInfo.add(new DecoratorBuildItem(KUBERNETES,
                        new AddNodePortDecorator(name, entry.getValue().nodePort.getAsInt(), entry.getKey())));
            }
        } else {
            manifestInfo.add(new DecoratorBuildItem(flavor,
                    new AddNodePortDecorator(name,
                            config.getNodePort().orElseGet(
                                    () -> getStablePortNumberInRange(name, MIN_NODE_PORT_VALUE, MAX_NODE_PORT_VALUE)),
                            config.ingress.targetPort)));
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

    @Override
    protected Optional<Port> createPort(KubernetesConfig config, List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.getPort(ports, config);
    }

    @Override
    protected void doCheckEnabled(ApplicationInfoBuildItem applicationInfo, Capabilities capabilities, KubernetesConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(flavor, DEPLOYMENT, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION,
                        priority, true, config.getDeployStrategy()));

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        resourceMeta.produce(
                new KubernetesResourceMetadataBuildItem(deploymentTarget, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION, DEPLOYMENT,
                        name));
    }

    protected List<ConfiguratorBuildItem> doCreateConfigurators(KubernetesConfig config,
            List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.combinePorts(ports, config).values().stream()
                .map(value -> new ConfiguratorBuildItem(new AddPortToKubernetesConfig(value)))
                .toList();
    }

    protected List<DecoratorBuildItem> doCreateDecorators(
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
            Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            List<KubernetesPortBuildItem> ports,
            Optional<KubernetesProbePortNameBuildItem> portName,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            Optional<KubernetesHealthStartupPathBuildItem> startupPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesClusterRoleBuildItem> clusterRoles,
            List<KubernetesServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            BuildProducer<KubernetesEffectiveServiceAccountBuildItem> serviceAccountProducer) {

        final var manifestInfo = doCreateDecorators(applicationInfo, outputTarget, config, packageConfig, metricsConfiguration,
                kubernetesClientConfiguration, namespaces, annotations, labels, envs, image, command, ports, livenessPath,
                readinessPath, startupPath, roles, clusterRoles, serviceAccounts, roleBindings, customProjectRoot, List.of());

        final var name = manifestInfo.getDefaultName();
        image.ifPresent(i -> manifestInfo
                .add(new DecoratorBuildItem(flavor, new ApplyContainerImageDecorator(name, i.getImage()))));
        manifestInfo.add(new DecoratorBuildItem(flavor, new ApplyImagePullPolicyDecorator(name, "IfNotPresent")));

        //Service handling
        serviceDecorators(config, manifestInfo);

        //Probe port handling
        probePortDecorators(config, manifestInfo, portName, ports);

        // Handle init Containers
        initContainersAndJobsDecorators(config, manifestInfo, initContainers, jobs);

        // Do not bind the Management port to the Service resource unless it's explicitly used by the user.
        managementPortDecorators(config, manifestInfo);

        return manifestInfo.getDecoratorsAndProduceServiceAccountBuildItem(serviceAccountProducer);
    }
}
