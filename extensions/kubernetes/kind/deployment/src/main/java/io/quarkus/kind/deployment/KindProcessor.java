package io.quarkus.kind.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KIND;
import static io.quarkus.kubernetes.deployment.Constants.KUBERNETES;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.List;
import java.util.Optional;

import io.quarkus.container.spi.BaseImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.util.ExecUtil;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.kubernetes.deployment.DevClusterProcessor;
import io.quarkus.kubernetes.deployment.KubernetesConfig;
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

public class KindProcessor extends DevClusterProcessor {

    private static final int KIND_PRIORITY = DEFAULT_PRIORITY + 30;

    public KindProcessor() {
        super(KIND, KIND_PRIORITY, KUBERNETES);
    }

    @BuildStep
    public void checkKind(ApplicationInfoBuildItem applicationInfo, KubernetesConfig config,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        doCheckEnabled(applicationInfo, null, config, deploymentTargets, resourceMeta);
    }

    @BuildStep
    public void createAnnotations(KubernetesConfig config, BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        doCreateAnnotations(config, annotations);
    }

    @BuildStep
    public void createLabels(KubernetesConfig config, BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        doCreateLabels(config, labels, imageLabels);
        // todo: should that label be added to all flavors?
        labels.produce(new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", KIND));
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(KubernetesConfig config,
            List<KubernetesPortBuildItem> ports) {
        return doCreateConfigurators(config, ports);
    }

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
            List<KubernetesServiceAccountBuildItem> serviceAccounts,
            List<KubernetesRoleBindingBuildItem> roleBindings,
            Optional<CustomProjectRootBuildItem> customProjectRoot,
            BuildProducer<KubernetesEffectiveServiceAccountBuildItem> serviceAccountProducer) {

        return doCreateDecorators(applicationInfo, outputTarget, config, packageConfig,
                metricsConfiguration, kubernetesClientConfiguration, namespaces, initContainers, jobs, annotations, labels,
                envs,
                baseImage, image, command, ports, portName,
                livenessPath, readinessPath, startupPath,
                roles, clusterRoles, serviceAccounts, roleBindings, customProjectRoot, serviceAccountProducer);
    }

    @BuildStep
    public void postBuild(ContainerImageInfoBuildItem image, List<ContainerImageBuilderBuildItem> builders,
            @SuppressWarnings("unused") BuildProducer<ArtifactResultBuildItem> artifactResults) {
        //We used to only perform the action below when using known builders that play nicely with kind (e.g. docker)
        //However, this excluded users that are just using external tools for building including the cli (e.g. quarkus image build docker).
        //So, we now always perform this step
        ExecUtil.exec("kind", "load", "docker-image", image.getImage());
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
