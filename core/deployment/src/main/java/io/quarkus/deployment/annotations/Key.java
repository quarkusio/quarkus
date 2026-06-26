package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code String} element as a key dimension for per-key iteration of build steps.
 * <p>
 * On a <b>{@link BuildStep @BuildStep} method parameter</b> of type {@code String},
 * this annotation enables per-key iteration: the engine discovers all unique key values
 * from keyed {@link io.quarkus.builder.item.MultiBuildItem} lists consumed by the method,
 * then invokes the method once per key value, injecting the current key and filtering
 * keyed lists to only items whose key matches.
 * <p>
 * On a <b>{@link io.quarkus.builder.item.MultiBuildItem}</b> field, this annotation marks
 * which field carries the key value. When such an item is consumed as a {@code List} by a
 * build step method with a {@code @Key} parameter of the same key type, the engine
 * automatically filters the list to only items whose key value matches.
 * A keyed {@code MultiBuildItem} may also be consumed as a bare parameter (not wrapped in
 * {@code List}); the engine filters to matching items and injects a single instance.
 * <p>
 * The {@link #value()} is any class that identifies this key dimension (the "key family").
 * Matching happens between build step parameters and build item fields that share the same
 * key type. The annotated element's value is the "key value" (e.g., the datasource name).
 * <p>
 * Only one {@code @Key} parameter is allowed per {@code @BuildStep} method, and only one
 * {@code @Key} field is allowed per {@code MultiBuildItem} class.
 * The annotated element must be of type {@code String}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface Key {

    /**
     * The class that identifies this key dimension.
     *
     * @return the key family class
     */
    Class<?> value();
}
