package io.quarkus.minikube.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_GROUP;
import static io.quarkus.kubernetes.deployment.Constants.DEPLOYMENT_VERSION;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.kubernetes.deployment.AddPortToKubernetesConfig;
import io.quarkus.kubernetes.deployment.DevClusterHelper;
import io.quarkus.kubernetes.deployment.InitTaskProcessor;
import io.quarkus.kubernetes.deployment.KubernetesCommonHelper;
import io.quarkus.kubernetes.deployment.KubernetesConfig;
import io.quarkus.kubernetes.deployment.ResourceNameUtil;
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

public class MinikubeProcessor {

    private static final int MINIKUBE_PRIORITY = DEFAULT_PRIORITY + 20;

    @BuildStep
    public void checkMinikube(ApplicationInfoBuildItem applicationInfo, KubernetesConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(MINIKUBE, DEPLOYMENT, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION,
                        MINIKUBE_PRIORITY, true, config.getDeployStrategy()));

        String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        resourceMeta.produce(
                new KubernetesResourceMetadataBuildItem(KUBERNETES, DEPLOYMENT_GROUP, DEPLOYMENT_VERSION, DEPLOYMENT, name));
    }

    @BuildStep
    public void createAnnotations(KubernetesConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config.getAnnotations().forEach((k, v) -> annotations.produce(new KubernetesAnnotationBuildItem(k, v, MINIKUBE)));
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
        KubernetesCommonHelper.combinePorts(ports, config).values()
                .forEach(value -> result.add(new ConfiguratorBuildItem(new AddPortToKubernetesConfig(value))));
        return result;
    }

    @BuildStep
    public KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(ApplicationInfoBuildItem applicationInfo,
            KubernetesConfig config, List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        return KubernetesCommonHelper.computeEffectiveServiceAccount(name, MINIKUBE,
                config, serviceAccountsFromExtensions,
                decorators);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
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
            List<KubernetesEffectiveServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            List<KubernetesClusterRoleBindingBuildItem> clusterRoleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot) {

        return DevClusterHelper.createDecorators(MINIKUBE, KUBERNETES, applicationInfo, outputTarget, config, packageConfig,
                metricsConfiguration, kubernetesClientConfiguration, namespaces, initContainers, jobs, annotations, labels,
                envs,
                baseImage, image, command, ports, portName,
                livenessPath, readinessPath, startupPath,
                roles, clusterRoles, serviceAccounts, roleBindings, clusterRoleBindings, customProjectRoot);
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
        if (config.isExternalizeInit()) {
            InitTaskProcessor.process(MINIKUBE, name, image, initTasks, config.getInitTaskDefaults(), config.getInitTasks(),
                    jobs, initContainers, env, roles, roleBindings, serviceAccount, decorators);
        }
    }
}
