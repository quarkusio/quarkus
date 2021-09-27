
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.DEFAULT_S2I_IMAGE_NAME;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_CONFIG;
import static io.quarkus.kubernetes.deployment.Constants.HTTP_PORT;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;
import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_APP_RUNTIME;
import static io.quarkus.kubernetes.deployment.Constants.QUARKUS;
import static io.quarkus.kubernetes.deployment.OpenshiftConfig.OpenshiftFlavor.v3;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.dekorate.kubernetes.annotation.ServiceType;
import io.dekorate.kubernetes.config.EnvBuilder;
import io.dekorate.kubernetes.decorator.AddEnvVarDecorator;
import io.dekorate.kubernetes.decorator.AddLabelDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.RemoveFromSelectorDecorator;
import io.dekorate.kubernetes.decorator.RemoveLabelDecorator;
import io.dekorate.openshift.decorator.ApplyReplicasDecorator;
import io.dekorate.project.Project;
import io.dekorate.s2i.config.S2iBuildConfig;
import io.dekorate.s2i.config.S2iBuildConfigBuilder;
import io.dekorate.s2i.decorator.AddBuilderImageStreamResourceDecorator;
import io.dekorate.utils.Labels;
import io.quarkus.container.image.deployment.util.ImageUtil;
import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
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
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;

public class OpenshiftProcessor {

    private static final int OPENSHIFT_PRIORITY = DEFAULT_PRIORITY;

    @BuildStep
    public void checkOpenshift(BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets) {
        List<String> targets = KubernetesConfigUtil.getUserSpecifiedDeploymentTargets();
        deploymentTargets.produce(new KubernetesDeploymentTargetBuildItem(OPENSHIFT, DEPLOYMENT_CONFIG, OPENSHIFT_PRIORITY,
                targets.contains(OPENSHIFT)));
    }

    @BuildStep
    public void createAnnotations(OpenshiftConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.getAnnotations().forEach((k, v) -> {
            annotations.produce(new KubernetesAnnotationBuildItem(k, v, OPENSHIFT));
        });
    }

    @BuildStep
    public void createLabels(OpenshiftConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config.getLabels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, OPENSHIFT));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(ApplicationInfoBuildItem applicationInfo,
            OpenshiftConfig config, Capabilities capabilities, Optional<ContainerImageInfoBuildItem> image,
            List<KubernetesPortBuildItem> ports) {

        List<ConfiguratorBuildItem> result = new ArrayList<>();

        KubernetesCommonHelper.combinePorts(ports, config).entrySet().forEach(e -> {
            result.add(new ConfiguratorBuildItem(new AddPortToOpenshiftConfig(e.getValue())));
        });
        result.add(new ConfiguratorBuildItem(new ApplyExpositionConfigurator(config.route)));

        if (!capabilities.isPresent(Capability.CONTAINER_IMAGE_S2I)
                && !capabilities.isPresent("io.quarkus.openshift")
                && !capabilities.isPresent(Capability.CONTAINER_IMAGE_OPENSHIFT)) {
            result.add(new ConfiguratorBuildItem(new DisableS2iConfigurator()));

            image.flatMap(ContainerImageInfoBuildItem::getRegistry).ifPresent(r -> {
                result.add(new ConfiguratorBuildItem(new ApplyImageRegistryConfigurator(r)));
            });

            image.map(ContainerImageInfoBuildItem::getGroup).ifPresent(g -> {
                result.add(new ConfiguratorBuildItem(new ApplyImageGroupConfigurator(g)));
            });
        }
        return result;
    }

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
            OutputTargetBuildItem outputTarget,
            OpenshiftConfig config,
            PackageConfig packageConfig,
            Optional<MetricsCapabilityBuildItem> metricsConfiguration,
            Capabilities capabilities,
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
            List<KubernetesRoleBindingBuildItem> roleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot) {

        List<DecoratorBuildItem> result = new ArrayList<>();
        String name = ResourceNameUtil.getResourceName(config, applicationInfo);

        Optional<Project> project = KubernetesCommonHelper.createProject(applicationInfo, customProjectRoot, outputTarget,
                packageConfig);
        result.addAll(KubernetesCommonHelper.createDecorators(project, OPENSHIFT, name, config,
                metricsConfiguration,
                annotations, labels, command,
                ports, livenessPath, readinessPath, roles, roleBindings));

        if (config.flavor == v3) {
            //Openshift 3.x doesn't recognize 'app.kubernetes.io/name', it uses 'app' instead.
            //The decorator will be applied even on non-openshift resources is it may affect for example: knative
            result.add(new DecoratorBuildItem(new AddLabelDecorator(name, "app", name)));

            // The presence of optional is causing issues in OCP 3.11, so we better remove them.
            // The following 4 decorator will set the optional property to null, so that it won't make it into the file.
            //The decorators will be applied even on non-openshift resources is they may affect for example: knative
            result.add(new DecoratorBuildItem(new RemoveOptionalFromSecretEnvSourceDecorator()));
            result.add(new DecoratorBuildItem(new RemoveOptionalFromConfigMapEnvSourceDecorator()));
            result.add(new DecoratorBuildItem(new RemoveOptionalFromSecretKeySelectorDecorator()));
            result.add(new DecoratorBuildItem(new RemoveOptionalFromConfigMapKeySelectorDecorator()));
        }

        if (config.getReplicas() != 1) {
            result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyReplicasDecorator(name, config.getReplicas())));
        }
        image.ifPresent(i -> {
            result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyContainerImageDecorator(name, i.getImage())));
        });
        result.add(new DecoratorBuildItem(OPENSHIFT, new AddLabelDecorator(name, OPENSHIFT_APP_RUNTIME, QUARKUS)));

        Stream.concat(config.convertToBuildItems().stream(),
                envs.stream().filter(e -> e.getTarget() == null || OPENSHIFT.equals(e.getTarget()))).forEach(e -> {
                    result.add(new DecoratorBuildItem(OPENSHIFT,
                            new AddEnvVarDecorator(ApplicationContainerDecorator.ANY, name,
                                    new EnvBuilder().withName(EnvConverter.convertName(e.getName())).withValue(e.getValue())
                                            .withSecret(e.getSecret()).withConfigmap(e.getConfigMap()).withField(e.getField())
                                            .build())));
                });

        // Handle custom s2i builder images
        baseImage.map(BaseImageInfoBuildItem::getImage).ifPresent(builderImage -> {
            String builderImageName = ImageUtil.getName(builderImage);
            S2iBuildConfig s2iBuildConfig = new S2iBuildConfigBuilder().withBuilderImage(builderImage).build();
            if (!DEFAULT_S2I_IMAGE_NAME.equals(builderImageName)) {
                result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveBuilderImageResourceDecorator(DEFAULT_S2I_IMAGE_NAME)));
            }
            result.add(new DecoratorBuildItem(OPENSHIFT, new AddBuilderImageStreamResourceDecorator(s2iBuildConfig)));
            result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyBuilderImageDecorator(name, builderImage)));
        });

        if (!config.addVersionToLabelSelectors) {
            result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveLabelDecorator(name, Labels.VERSION)));
            result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveFromSelectorDecorator(name, Labels.VERSION)));
        }

        // Service handling
        result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyServiceTypeDecorator(name, config.getServiceType().name())));
        if ((config.getServiceType() == ServiceType.NodePort) && config.nodePort.isPresent()) {
            result.add(new DecoratorBuildItem(OPENSHIFT, new AddNodePortDecorator(name, config.nodePort.getAsInt())));
        }

        // Probe port handling
        Integer port = ports.stream().filter(p -> HTTP_PORT.equals(p.getName())).map(KubernetesPortBuildItem::getPort)
                .findFirst().orElse(DEFAULT_HTTP_PORT);
        result.add(new DecoratorBuildItem(OPENSHIFT, new ApplyHttpGetActionPortDecorator(name, name, port)));

        // Handle non-s2i
        if (!capabilities.isPresent(Capability.CONTAINER_IMAGE_S2I)
                && !capabilities.isPresent("io.quarkus.openshift")
                && !capabilities.isPresent(Capability.CONTAINER_IMAGE_OPENSHIFT)) {
            result.add(new DecoratorBuildItem(OPENSHIFT, new RemoveDeploymentTriggerDecorator()));
        }

        return result;
    }
}
