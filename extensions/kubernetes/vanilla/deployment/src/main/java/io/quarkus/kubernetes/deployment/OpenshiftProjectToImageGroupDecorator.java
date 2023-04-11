package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT_INTERNAL_REGISTRY;

import java.util.Optional;

import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.fabric8.kubernetes.api.model.ContainerFluent;
import io.fabric8.kubernetes.client.Config;
import io.quarkus.container.image.deployment.util.ImageUtil;

public class OpenshiftProjectToImageGroupDecorator extends ApplicationContainerDecorator<ContainerFluent<?>>
        implements DeploymentDecorator<OpenshiftProjectToImageGroupDecorator> {

    private final String name;
    private final String namespace;

    private Optional<String> message;

    public OpenshiftProjectToImageGroupDecorator(String name) {
        this(name, ANY);
    }

    public OpenshiftProjectToImageGroupDecorator(String name, String namespace) {
        super(ANY, ANY);
        this.name = name;
        this.namespace = namespace;
    }

    @Override
    public void andThenVisit(ContainerFluent<?> container) {
        if (namespace == null || namespace.isEmpty()) {
            return;
        }
        String image = container.getImage();
        Optional<String> registry = ImageUtil.getRegistry(image);
        String group = ImageUtil.getGroup(image);
        if (!group.equals(namespace)
                && name.equals(ImageUtil.getName(image))
                && registry.map(r -> r.equals(OPENSHIFT_INTERNAL_REGISTRY)).orElse(false)) {

            String newImage = ImageUtil.getImage(registry, namespace, name, ImageUtil.getTag(image));
            container.withImage(newImage);
            message = Optional.of("Changed image to: " + newImage
                    + " so that it matches the internal registry of Openshift. This feature can be disabled using the flag `quarkus.openshift.project-as-image-group`.");
        }
    }

    @Override
    public boolean isApplied() {
        return message.isPresent();
    }

    @Override
    public Optional<String> getMessage() {
        return message;
    }

    @Override
    public OpenshiftProjectToImageGroupDecorator withDeploymentContext(Config config) {
        return new OpenshiftProjectToImageGroupDecorator(name, config.getNamespace());
    }
}
