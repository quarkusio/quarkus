package io.quarkus.builder.item;

/**
 * An empty build item. Empty build items carry no data and may be used, for example, for ordering and for
 * running steps which don't otherwise produce anything.
 *
 * Empty build items cannot be instantiated, you must use <code>@Produce(MyEmptyBuildItem.class)</code> or
 * <code>@Consume(MyEmptyBuildItem.class)</code> instead of the standard ways to consume or produce build items.
 */
public abstract class EmptyBuildItem extends BuildItem {
    protected EmptyBuildItem() {
        throw new UnsupportedOperationException("Cannot construct empty build items");
    }
}
