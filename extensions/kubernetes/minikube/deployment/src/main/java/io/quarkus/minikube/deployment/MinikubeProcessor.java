package io.quarkus.minikube.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.MAX_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MAX_PORT_NUMBER;
import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;
import static io.quarkus.kubernetes.deployment.Constants.MIN_NODE_PORT_VALUE;
import static io.quarkus.kubernetes.deployment.Constants.MIN_PORT_NUMBER;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImagePullPolicyDecorator;
import io.dekorate.project.Project;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.deployment.AddNodePortDecorator;
import io.quarkus.kubernetes.deployment.AddPortToKubernetesConfig;
import io.quarkus.kubernetes.deployment.ApplyContainerImageDecorator;
import io.quarkus.kubernetes.deployment.ApplyHttpGetActionPortDecorator;
import io.quarkus.kubernetes.deployment.ApplyServiceTypeDecorator;
import io.quarkus.kubernetes.deployment.EnvConverter;
import io.quarkus.kubernetes.deployment.KubernetesCommonHelper;
import io.quarkus.kubernetes.deployment.KubernetesConfig;
import io.quarkus.kubernetes.deployment.PortConfig;
import io.quarkus.kubernetes.deployment.ResourceNameUtil;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesCommandBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthLivenessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesHealthReadinessPathBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public class MinikubeProcessor {

    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";
    private static final int MINIKUBE_PRIORITY = DEFAULT_PRIORITY + 20;

    @BuildStep
    public void checkMinikube(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(MINIKUBE, DEPLOYMENT, MINIKUBE_PRIORITY, true));
    }

    @BuildStep
    public void createAnnotations(KubernetesConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.getAnnotations().forEach((k, v) -> {
            annotations.produce(new KubernetesAnnotationBuildItem(k, v, MINIKUBE));
        });
    }

    @BuildStep
    public void createLabels(KubernetesConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config.getLabels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, MINIKUBE));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(KubernetesConfig config,
            List<KubernetesPortBuildItem> ports) {
        List<ConfiguratorBuildItem> result = new ArrayList<>();
        KubernetesCommonHelper.combinePorts(ports, config).entrySet().forEach(e -> {
            result.add(new ConfiguratorBuildItem(new AddPortToKubernetesConfig(e.getValue())));
        });
        return result;
    }

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            KubernetesConfig config,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            List<KubernetesAnnotationBuildItem> annotations,
            List<KubernetesLabelBuildItem> labels,
            List<KubernetesEnvBuildItem> envs,
            Optional<BaseImageInfoBuildItem> baseImage,
            Optional<ContainerImageInfoBuildItem> image,
            Optional<KubernetesCommandBuildItem> command,
            List<KubernetesPortBuildItem> ports,
            Optional<KubernetesHealthLivenessPathBuildItem> livenessPath,
            Optional<KubernetesHealthReadinessPathBuildItem> readinessPath,
            List<KubernetesRoleBuildItem> roles,
            List<KubernetesRoleBindingBuildItem> roleBindings) {

        List<DecoratorBuildItem> result = new ArrayList<>();
        String name = ResourceNameUtil.getResourceName(config, applicationInfo);

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, outputTarget, packageConfig);
        result.addAll(KubernetesCommonHelper.createDecorators(project, MINIKUBE, name, config,
                metricsConfiguration,
                annotations, labels, command,
                ports, livenessPath, readinessPath, roles, roleBindings));

        image.ifPresent(i -> {
            result.add(new DecoratorBuildItem(MINIKUBE, new ApplyContainerImageDecorator(name, i.getImage())));
        });

        Stream.concat(config.convertToBuildItems().stream(),
                envs.stream().filter(e -> e.getTarget() == null || KUBERNETES.equals(e.getTarget()))).forEach(e -> {
                    result.add(new DecoratorBuildItem(MINIKUBE,
                            new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name, new EnvBuilder()
                                    .withName(EnvConverter.convertName(e.getName()))
                                    .withValue(e.getValue())
                                    .withSecret(e.getSecret())
                                    .withConfigmap(e.getConfigMap())
                                    .withField(e.getField())
                                    .build())));
                });

        result.add(new DecoratorBuildItem(MINIKUBE, new ApplyImagePullPolicyDecorator(name, "IfNotPresent")));

        //Service handling
        result.add(new DecoratorBuildItem(MINIKUBE, new ApplyServiceTypeDecorator(name, ServiceType.NodePort.name())));
        List<Map.Entry<String, PortConfig>> nodeConfigPorts = config.getPorts().entrySet().stream()
                .filter(e -> e.getValue().nodePort.isPresent())
                .collect(Collectors.toList());
        if (!nodeConfigPorts.isEmpty()) {
            for (Map.Entry<String, PortConfig> entry : nodeConfigPorts) {
                result.add(new DecoratorBuildItem(KUBERNETES,
                        new AddNodePortDecorator(name, entry.getValue().nodePort.getAsInt(), Optional.of(entry.getKey()))));
            }
        } else {
            result.add(new DecoratorBuildItem(MINIKUBE, new AddNodePortDecorator(name, config.getNodePort()
                    .orElseGet(() -> getStablePortNumberInRange(name, MIN_NODE_PORT_VALUE, MAX_NODE_PORT_VALUE)))));
        }

        //Probe port handling
        Integer port = ports.stream().filter(p -> HTTP_PORT.equals(p.getName())).map(KubernetesPortBuildItem::getPort)
                .findFirst().orElse(DEFAULT_HTTP_PORT);
        result.add(new DecoratorBuildItem(MINIKUBE, new ApplyHttpGetActionPortDecorator(name, name, port)));

        return result;
    }

    /**
     * Given a string, generate a port number within the supplied range
     * The output is always the same (between {@code min} and {@code max})
     * given the same input and it's useful when we need to generate a port number
     * which needs to stay the same but we don't care about the exact value
     */
    private int getStablePortNumberInRange(String input, int min, int max) {
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
