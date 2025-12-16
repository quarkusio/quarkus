package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.dekorate.kubernetes.config.Port;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImageLabelBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.InitTaskBuildItem;
import io.quarkus.kubernetes.spi.ConfiguratorBuildItem;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesAnnotationBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentTargetBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEffectiveServiceAccountBuildItem;
import io.quarkus.kubernetes.spi.KubernetesEnvBuildItem;
import io.quarkus.kubernetes.spi.KubernetesInitContainerBuildItem;
import io.quarkus.kubernetes.spi.KubernetesJobBuildItem;
import io.quarkus.kubernetes.spi.KubernetesLabelBuildItem;
import io.quarkus.kubernetes.spi.KubernetesNamespaceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBindingBuildItem;
import io.quarkus.kubernetes.spi.KubernetesRoleBuildItem;
import io.quarkus.kubernetes.spi.KubernetesServiceAccountBuildItem;

public abstract class BaseKubeProcessor<P, C extends PlatformConfiguration> {

    protected abstract int priority();

    protected abstract String deploymentTarget();

    protected abstract P portConfigurator(Port port);

    protected abstract C config();

    protected String clusterType() {
        return KUBERNETES;
    }

    protected boolean enabled() {
        return true;
    }

    protected DeploymentResourceKind deploymentResourceKind(Capabilities capabilities) {
        return DeploymentResourceKind.Deployment;
    }

    protected void produceDeploymentBuildItem(ApplicationInfoBuildItem applicationInfo,
            Capabilities capabilities,
            BuildProducer<KubernetesDeploymentTargetBuildItem> deploymentTargets,
            BuildProducer<KubernetesResourceMetadataBuildItem> resourceMeta) {
        final var enabled = enabled();
        final var config = config();
        final var drk = deploymentResourceKind(capabilities);
        deploymentTargets.produce(
                new KubernetesDeploymentTargetBuildItem(deploymentTarget(), drk.getKind(), drk.getGroup(), drk.getVersion(),
                        priority(), enabled, config.deployStrategy()));

        if (enabled) {
            String name = ResourceNameUtil.getResourceName(config, applicationInfo);
            resourceMeta.produce(
                    new KubernetesResourceMetadataBuildItem(clusterType(), drk.getGroup(), drk.getVersion(), drk.getKind(),
                            name));
        }
    }

    protected void createAnnotations(BuildProducer<KubernetesAnnotationBuildItem> annotations) {
        config().annotations()
                .forEach((k, v) -> annotations.produce(new KubernetesAnnotationBuildItem(k, v, deploymentTarget())));
    }

    protected void createLabels(BuildProducer<KubernetesLabelBuildItem> labels,
            BuildProducer<ContainerImageLabelBuildItem> imageLabels) {
        config().labels().forEach((k, v) -> {
            labels.produce(new KubernetesLabelBuildItem(k, v, deploymentTarget()));
            imageLabels.produce(new ContainerImageLabelBuildItem(k, v));
        });
        labels.produce(
                new KubernetesLabelBuildItem(KubernetesLabelBuildItem.CommonLabels.MANAGED_BY, "quarkus", deploymentTarget()));
    }

    protected Stream<Port> asStream(List<KubernetesPortBuildItem> ports) {
        return KubernetesCommonHelper.combinePorts(ports, config()).values().stream();
    }

    protected List<ConfiguratorBuildItem> createConfigurators(List<KubernetesPortBuildItem> ports) {
        return asStream(ports)
                .map(port -> new ConfiguratorBuildItem(portConfigurator(port)))
                .collect(Collectors.toCollection(ArrayList::new)); // need a mutable list so sub-classes can add to it
    }

    protected KubernetesEffectiveServiceAccountBuildItem computeEffectiveServiceAccounts(
            ApplicationInfoBuildItem applicationInfo,
            List<KubernetesServiceAccountBuildItem> serviceAccountsFromExtensions,
            BuildProducer<DecoratorBuildItem> decorators) {
        final var config = config();
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        return KubernetesCommonHelper.computeEffectiveServiceAccount(name, deploymentTarget(),
                config, serviceAccountsFromExtensions,
                decorators);
    }

    protected void createNamespace(BuildProducer<KubernetesNamespaceBuildItem> namespace) {
        config().namespace().ifPresent(n -> namespace.produce(new KubernetesNamespaceBuildItem(deploymentTarget(), n)));
    }

    protected void externalizeInitTasks(
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
        final var config = config();
        final String name = ResourceNameUtil.getResourceName(config, applicationInfo);
        if (config.externalizeInit()) {
            InitTaskProcessor.process(deploymentTarget(), name, image, initTasks, config.initTaskDefaults(), config.initTasks(),
                    jobs, initContainers, env, roles, roleBindings, serviceAccount, decorators);
        }
    }
}
