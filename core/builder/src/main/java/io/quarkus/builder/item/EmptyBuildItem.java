package io.quarkus.builder.item;

/**
 * An empty build item. Empty build items carry no data and may be used, for example, for ordering and for
 * running steps which don't otherwise produce anything.
 */
public abstract class EmptyBuildItem extends BuildItem {
    protected EmptyBuildItem() {
        throw new UnsupportedOperationException("Cannot construct empty build items");
    }
}
