package io.quarkus.container.image.openshift.deployment;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;

/**
 * Responsible for finding an ImageStream with the given name and renaming it. This allows
 * changing the name of the output image when saving to the internal registry.
 */
public class ApplyImageNameDecorator extends NamedResourceDecorator<ObjectMetaFluent<?>> {

    private final String newName;

    public ApplyImageNameDecorator(String currentName, String newName) {
        super("ImageStream", currentName);
        this.newName = newName;
    }

    @Override
    public void andThenVisit(ObjectMetaFluent<?> meta, ObjectMeta resourceMeta) {
        meta.withName(newName);
    }

}
