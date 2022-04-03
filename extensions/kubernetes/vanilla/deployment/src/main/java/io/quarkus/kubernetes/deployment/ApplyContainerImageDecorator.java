package io.quarkus.kubernetes.deployment;

import static io.dekorate.ConfigReference.generateConfigReferenceName;

import java.util.Arrays;
import java.util.List;

import io.dekorate.ConfigReference;
import io.dekorate.WithConfigReferences;
import io.dekorate.kubernetes.decorator.AddInitContainerDecorator;
import io.dekorate.kubernetes.decorator.AddSidecarDecorator;
import io.dekorate.kubernetes.decorator.ApplicationContainerDecorator;
import io.dekorate.kubernetes.decorator.ApplyImageDecorator;
import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.utils.Strings;
import io.fabric8.kubernetes.api.model.ContainerFluent;

/**
 * A decorator for applying an image to a container capable of overriding the internal {@link ApplyImageDecorator}.
 */
public class ApplyContainerImageDecorator extends ApplicationContainerDecorator<ContainerFluent>
        implements WithConfigReferences {
    private final String image;

    public ApplyContainerImageDecorator(String containerName, String image) {
        super((String) null, containerName);
        this.image = image;
    }

    public ApplyContainerImageDecorator(String deploymentName, String containerName, String image) {
        super(deploymentName, containerName);
        this.image = image;
    }

    public void andThenVisit(ContainerFluent container) {
        container.withImage(this.image);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { ResourceProvidingDecorator.class, AddSidecarDecorator.class, AddInitContainerDecorator.class,
                ApplyImageDecorator.class };
    }

    @Override
    public List<ConfigReference> getConfigReferences() {
        return Arrays.asList(buildConfigReferenceForImage());
    }

    private ConfigReference buildConfigReferenceForImage() {
        String property = generateConfigReferenceName("image", getContainerName(), getDeploymentName());
        String jsonPath = "$..spec.template.spec.containers..image";
        if (!Strings.equals(getDeploymentName(), ANY) && !Strings.equals(getContainerName(), ANY)) {
            jsonPath = "$.[?(@.metadata.name == '" + getDeploymentName() + "')].spec.template.spec.containers[?(@.name == '"
                    + getContainerName() + "')].image";
        } else if (!Strings.equals(getDeploymentName(), ANY)) {
            jsonPath = "$.[?(@.metadata.name == '" + getDeploymentName() + "')].spec.template.spec.containers..image";
        } else if (!Strings.equals(getContainerName(), ANY)) {
            jsonPath = "$..spec.template.spec.containers[?(@.name == '" + getContainerName() + "')].image";
        }

        return new ConfigReference(property, jsonPath, image);
    }

}
