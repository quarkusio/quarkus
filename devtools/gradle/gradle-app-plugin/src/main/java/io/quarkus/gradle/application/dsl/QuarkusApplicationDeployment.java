package io.quarkus.gradle.application.dsl;

import static java.util.Objects.requireNonNull;

import org.gradle.api.Named;
import org.gradle.api.provider.Property;
import org.jspecify.annotations.NonNull;

import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentTarget;

/**
 * Base DSL element for a named deployment of one named application build.
 * <p>
 * The concrete subtype fixes the deployment target. The deployment conventionally consumes the normal named image
 * push; selecting another image source changes the task predecessor. An explicit image reference is optional and is
 * validated with the selected image-source policy before the deployment operation executes.
 */
public abstract class QuarkusApplicationDeployment implements Named {

    private final String name;
    private final QuarkusApplicationDeploymentTarget target;

    /**
     * Creates a deployment element with a fixed name and target.
     *
     * @param name the name used to derive the deployment task name
     * @param target the fixed deployment target
     */
    protected QuarkusApplicationDeployment(String name, QuarkusApplicationDeploymentTarget target) {
        this.name = requireNonNull(name, "name");
        this.target = requireNonNull(target, "target");
        getImageSource().convention(QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH);
    }

    /**
     * Returns the deployment element name.
     *
     * @return the immutable deployment name
     */
    @Override
    public @NonNull String getName() {
        return name;
    }

    /**
     * Returns the target selected by the concrete deployment type.
     *
     * @return the immutable deployment target
     */
    public QuarkusApplicationDeploymentTarget getTarget() {
        return target;
    }

    /**
     * Returns the image operation or external reference used as the deployment source.
     *
     * @return the image source, conventionally the normal image push
     */
    public abstract Property<QuarkusApplicationDeploymentImageSource> getImageSource();

    /**
     * Returns the optional external image reference used by image-source modes that require one.
     *
     * @return the image reference, unset by default
     */
    public abstract Property<String> getImageReference();
}
