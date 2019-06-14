package io.quarkus.builder.item;

/**
 * A build item that can occur more than once in a build, discriminated by name.
 *
 * @param <N> the name type
 */
@SuppressWarnings("unused")
public abstract class NamedBuildItem<N> extends BuildItem {
    NamedBuildItem() {
    }
}
