package io.quarkus.deployment.key;

/**
 * Describes a keyed {@link io.quarkus.builder.item.MultiBuildItem} parameter in a per-key build step method.
 *
 * @param index the parameter index in the method's argument array
 * @param descriptor the key descriptor for the build item class
 * @param singular {@code true} if the parameter is a bare {@code MultiBuildItem} (not {@code List<>});
 *        the filtered list will be unwrapped to a single item at execution time
 */
public record KeyedParam(int index, KeyDescriptor descriptor, boolean singular) {
}
