package io.quarkus.gradle.application.dsl;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

/**
 * Configures container-image operations associated with one named application build.
 * <p>
 * All fields are optional and provider-backed. A complete image reference is mutually exclusive with repository or tag
 * components. A repository without a tag uses {@code project.version}; that default is invalid when the project version
 * is {@code unspecified}. The builder selects the corresponding Quarkus container-image extension. Image-specific
 * Quarkus properties override matching root and named-build properties for image operations.
 */
public abstract class QuarkusApplicationImage {

    /**
     * Creates an image configuration with all scalar properties unset.
     */
    public QuarkusApplicationImage() {
    }

    /**
     * Returns the optional complete image reference, such as {@code quay.io/acme/orders:1.0}.
     *
     * @return the complete image reference, unset by default
     */
    public abstract Property<String> getImageReference();

    /**
     * Returns the optional image repository used when no complete reference is supplied.
     *
     * @return the image repository, unset by default
     */
    public abstract Property<String> getRepository();

    /**
     * Returns the optional image tag used when no complete reference is supplied. With a repository it defaults to
     * {@code project.version}; without a repository it is forwarded as Quarkus's container-image tag.
     *
     * @return the image tag, unset by default
     */
    public abstract Property<String> getTag();

    /**
     * Returns the optional container-image builder selection.
     *
     * @return the image builder, unset by default
     */
    public abstract Property<QuarkusApplicationImageBuilder> getBuilder();

    /**
     * Returns Quarkus configuration applied only to this named build's image operations.
     *
     * @return the lazily configurable image-operation properties, empty by default
     */
    public abstract MapProperty<String, String> getQuarkusBuildProperties();
}
