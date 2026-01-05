package io.quarkus.kind.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KIND;
import static io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem.DEFAULT_PRIORITY;

import java.util.List;
import java.util.Optional;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.kubernetes.client.spi.KubernetesClientCapabilityBuildItem;
import io.quarkus.kubernetes.deployment.BaseVanillaKubernetesProcessor;
import io.quarkus.kubernetes.deployment.KubernetesConfig;
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
import io.smallrye.common.process.AbnormalExitException;
import io.smallrye.common.process.ProcessBuilder;

public class KindProcessor extends BaseVanillaKubernetesProcessor {
    private KubernetesConfig config;

    @Override
    protected KubernetesConfig config() {
        return config;
    }

    @Override
    protected int priority() {
        return DEFAULT_PRIORITY + 30;
    }

    @Override
    protected String deploymentTarget() {
        return KIND;
    }

    @Override
    protected boolean isDeploymentTargetDisabled(List<KubernetesDeploymentTargetBuildItem> targets) {
        return false;
    }

    protected ImagePullPolicy pullPolicy() {
        return ImagePullPolicy.IfNotPresent;
    }

    @BuildStep
    public void checkKind(ApplicationInfoBuildItem applicationInfo,
            Capabilities capabilities,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        super.produceDeploymentBuildItem(applicationInfo, capabilities, deploymentTargets, resourceMeta);
    }

    @BuildStep
    public void createAnnotations(BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        super.createAnnotations(annotations);
    }

    @BuildStep
    public void createLabels(BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        super.createLabels(labels, imageLabels);
    }

    @BuildStep
    public List<ConfiguratorBuildItem> createConfigurators(List<KubernetesPortBuildItem> ports) {
        return super.createConfigurators(ports);
    }

    @BuildStep
    public KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(ApplicationInfoBuildItem applicationInfo,
            List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        return super.computeEffectiveServiceAccounts(applicationInfo, serviceAccountsFromExtensions, decorators);
    }

    @BuildStep
    public List<DecoratorBuildItem> createDecorators(ApplicationInfoBuildItem applicationInfo,
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
        return super.decorators(applicationInfo, outputTarget,
                packageConfig,
                metricsConfiguration, kubernetesClientConfiguration, namespaces, initContainers, jobs, annotations, labels,
                envs,
                image, command, ports, portName,
                livenessPath, readinessPath, startupPath,
                roles, clusterRoles, serviceAccounts, roleBindings, clusterRoleBindings, customProjectRoot, targets)
                .decorators();
    }

    @BuildStep
    public void externalizeInitTasks(
            ApplicationInfoBuildItem applicationInfo,
            ContainerImageInfoBuildItem image,
            List<InitTaskBuildItem> initTasks,
            BuildProducer<KubernetesJobBuildItem> jobs,
            BuildProducer<KubernetesInitContainerBuildItem> initContainers,
            BuildProducer<KubernetesEnvBuildItem> env,
            BuildProducer<KubernetesRoleBuildItem> roles,
            BuildProducer<KubernetesRoleBindingBuildItem> roleBindings,
            BuildProducer<KubernetesServiceAccountBuildItem> serviceAccount,
            BuildProducer<DecoratorBuildItem> decorators) {
        super.externalizeInitTasks(applicationInfo, image, initTasks, jobs, initContainers, env, roles, roleBindings,
                serviceAccount, decorators);
    }

    @BuildStep
    public void postBuild(ContainerImageInfoBuildItem image,
            @SuppressWarnings("unused") List<ContainerImageBuilderBuildItem> builders,
            @SuppressWarnings("unused") BuildProducer<ArtifactResultBuildItem> artifactResults) {
        //We used to only perform the action below when using known builders that play nicely with kind (e.g. docker)
        //However, this excluded users that are just using external tools for building including the cli (e.g. quarkus image build docker).
        //So, we now always perform this step
        try {
            ProcessBuilder.exec("kind", "load", "docker-image", image.getImage());
        } catch (AbnormalExitException ignored) {
        }
    }
}
