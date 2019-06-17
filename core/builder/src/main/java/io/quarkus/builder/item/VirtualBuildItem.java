package io.quarkus.builder.item;

/**
 * A virtual build item. Virtual build items carry no data and may be used, for example, for ordering and for
 * running steps which don't otherwise produce anything.
 */
public abstract class VirtualBuildItem extends BuildItem {
    protected VirtualBuildItem() {
        throw new UnsupportedOperationException("Cannot construct virtual build items");
    }
}
