package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.MINIKUBE;

import java.util.Optional;

import io.dekorate.AbstractKubernetesManifestGenerator;
import io.dekorate.BuildServiceFactories;
import io.dekorate.ConfigurationRegistry;
import io.dekorate.ResourceRegistry;
import io.dekorate.WithProject;
import io.dekorate.config.ConfigurationSupplier;
import io.dekorate.kubernetes.config.Configuration;
import io.dekorate.kubernetes.config.Container;
import io.dekorate.kubernetes.config.EditableKubernetesConfig;
import io.dekorate.kubernetes.config.ImageConfiguration;
import io.dekorate.kubernetes.config.ImageConfigurationBuilder;
import io.dekorate.kubernetes.config.KubernetesConfig;
import io.dekorate.kubernetes.config.KubernetesConfigBuilder;
import io.dekorate.kubernetes.configurator.ApplyDeployToApplicationConfiguration;
import io.dekorate.kubernetes.decorator.AddIngressDecorator;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddServiceResourceDecorator;
import io.dekorate.kubernetes.decorator.ApplyHeadlessDecorator;
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.ApplyReplicasDecorator;
import io.dekorate.project.ApplyProjectInfo;
import io.dekorate.project.Project;
import io.dekorate.utils.Images;
import io.dekorate.utils.Labels;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

// copied from KubernetesHandler
// TODO: update dekorate to make KubernetesHandler extendable
public class MinikubeManifestGenerator extends AbstractKubernetesManifestGenerator<KubernetesConfig> implements WithProject {

    private static final String DEFAULT_REGISTRY = "docker.io";

    private static final String IF_NOT_PRESENT = "IfNotPresent";
    private static final String KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    private static final String METADATA_NAMESPACE = "metadata.namespace";
    private static final String MINIKUBE = "minikube";

    private final ConfigurationRegistry configurationRegistry;

    public MinikubeManifestGenerator(ResourceRegistry resourceRegistry, ConfigurationRegistry configurationRegistry) {
        super(resourceRegistry);
        this.configurationRegistry = configurationRegistry;
    }

    @Override
    public String getKey() {
        return MINIKUBE;
    }

    @Override
    public int order() {
        return 210;
    }

    public void generate(KubernetesConfig config) {
        ImageConfiguration imageConfig = getImageConfiguration(getProject(), config, configurationRegistry);

        Optional<Deployment> existingDeployment = resourceRegistry.groups().getOrDefault(MINIKUBE, new KubernetesListBuilder())
                .buildItems().stream()
                .filter(i -> i instanceof Deployment)
                .map(i -> (Deployment) i)
                .filter(i -> i.getMetadata().getName().equals(config.getName()))
                .findAny();

        if (!existingDeployment.isPresent()) {
            resourceRegistry.add(MINIKUBE, createDeployment(config, imageConfig));
        }

        addDecorators(MINIKUBE, config);

        if (config.isHeadless()) {
            resourceRegistry.decorate(MINIKUBE, new ApplyHeadlessDecorator(config.getName()));
        }

        if (config.getReplicas() != 1) {
            resourceRegistry.decorate(MINIKUBE, new ApplyReplicasDecorator(config.getName(), config.getReplicas()));
        }

        String image = Strings.isNotNullOrEmpty(imageConfig.getImage())
                ? imageConfig.getImage()
                : Images.getImage(imageConfig.isAutoPushEnabled()
                        ? (Strings.isNullOrEmpty(imageConfig.getRegistry()) ? DEFAULT_REGISTRY : imageConfig.getRegistry())
                        : imageConfig.getRegistry(),
                        imageConfig.getGroup(), imageConfig.getName(), imageConfig.getVersion());

        resourceRegistry.decorate(MINIKUBE, new ApplyImageDecorator(config.getName(), image));
    }

    public boolean accepts(Class<? extends Configuration> type) {
        return type.equals(KubernetesConfig.class) ||
                type.equals(EditableKubernetesConfig.class);
    }

    @Override
    protected void addDecorators(String group, KubernetesConfig config) {
        super.addDecorators(group, config);

        for (Container container : config.getInitContainers()) {
            resourceRegistry.decorate(group, new AddInitContainerDecorator(config.getName(), container));
        }

        if (config.getPorts().length > 0) {
            resourceRegistry.decorate(group, new AddServiceResourceDecorator(config));
        }

        resourceRegistry.decorate(group, new AddIngressDecorator(config, Labels.createLabelsAsMap(config, "Ingress")));
    }

    /**
     * Creates a {@link Deployment} for the {@link KubernetesConfig}.
     * 
     * @param appConfig The session.
     * @return The deployment.
     */
    public Deployment createDeployment(KubernetesConfig appConfig, ImageConfiguration imageConfig) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(appConfig.getName())
                .withLabels(Labels.createLabelsAsMap(appConfig, "Deployment"))
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withTemplate(createPodTemplateSpec(appConfig, imageConfig))
                .withSelector(createSelector(appConfig))
                .endSpec()
                .build();
    }

    /**
     * Creates a {@link LabelSelector} that matches the labels for the {@link KubernetesConfig}.
     * 
     * @return A labels selector.
     */
    public LabelSelector createSelector(KubernetesConfig config) {
        return new LabelSelectorBuilder()
                .withMatchLabels(Labels.createLabelsAsMap(config, "Deployment"))
                .build();
    }

    /**
     * Creates a {@link PodTemplateSpec} for the {@link KubernetesConfig}.
     * 
     * @param appConfig The sesssion.
     * @return The pod template specification.
     */
    public static PodTemplateSpec createPodTemplateSpec(KubernetesConfig appConfig, ImageConfiguration imageConfig) {
        return new PodTemplateSpecBuilder()
                .withSpec(createPodSpec(appConfig, imageConfig))
                .withNewMetadata()
                .withLabels(Labels.createLabelsAsMap(appConfig, "Deployment"))
                .endMetadata()
                .build();
    }

    /**
     * Creates a {@link PodSpec} for the {@link KubernetesConfig}.
     * 
     * @param imageConfig The sesssion.
     * @return The pod specification.
     */
    public static PodSpec createPodSpec(KubernetesConfig appConfig, ImageConfiguration imageConfig) {
        String image = Images
                .getImage(
                        imageConfig.isAutoPushEnabled()
                                ? (Strings.isNullOrEmpty(imageConfig.getRegistry()) ? DEFAULT_REGISTRY
                                        : imageConfig.getRegistry())
                                : imageConfig.getRegistry(),
                        imageConfig.getGroup(), imageConfig.getName(), imageConfig.getVersion());

        return new PodSpecBuilder()
                .addNewContainer()
                .withName(appConfig.getName())
                .withImage(image)
                .withImagePullPolicy(IF_NOT_PRESENT)
                .addNewEnv()
                .withName(KUBERNETES_NAMESPACE)
                .withNewValueFrom()
                .withNewFieldRef(null, METADATA_NAMESPACE)
                .endValueFrom()
                .endEnv()
                .endContainer()
                .build();
    }

    @Override
    public ConfigurationSupplier<KubernetesConfig> getFallbackConfig() {
        Project p = getProject();
        return new ConfigurationSupplier<KubernetesConfig>(new KubernetesConfigBuilder()
                .accept(new ApplyDeployToApplicationConfiguration()).accept(new ApplyProjectInfo(p)));
    }

    private static ImageConfiguration getImageConfiguration(Project project, KubernetesConfig appConfig,
            ConfigurationRegistry configurationRegistry) {
        return configurationRegistry.getImageConfig(BuildServiceFactories.supplierMatches(project))
                .map(i -> merge(appConfig, i))
                .orElse(ImageConfiguration.from(appConfig));
    }

    private static ImageConfiguration merge(KubernetesConfig appConfig, ImageConfiguration imageConfig) {
        if (appConfig == null) {
            throw new NullPointerException("KubernetesConfig is null.");
        }
        if (imageConfig == null) {
            return ImageConfiguration.from(appConfig);
        }
        return new ImageConfigurationBuilder()
                .withProject(imageConfig.getProject() != null ? imageConfig.getProject() : appConfig.getProject())
                .withGroup(imageConfig.getGroup() != null ? imageConfig.getGroup() : null)
                .withName(imageConfig.getName() != null ? imageConfig.getName() : appConfig.getName())
                .withVersion(imageConfig.getVersion() != null ? imageConfig.getVersion() : appConfig.getVersion())
                .withRegistry(imageConfig.getRegistry() != null ? imageConfig.getRegistry() : null)
                .withDockerFile(imageConfig.getDockerFile() != null ? imageConfig.getDockerFile() : "Dockerfile")
                .withAutoBuildEnabled(imageConfig.isAutoBuildEnabled() ? imageConfig.isAutoBuildEnabled() : false)
                .withAutoPushEnabled(imageConfig.isAutoPushEnabled() ? imageConfig.isAutoPushEnabled() : false)
                .build();
    }

}
