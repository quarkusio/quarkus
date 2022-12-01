package io.quarkus.builder.item;

/**
 * A build item that may be produced multiple times, and consumed as a {@code List}. {@code MultiBuildItem} subclasses
 * which implement {@code Comparable} will be returned in sorted order.
 */
public abstract class MultiBuildItem extends BuildItem {
    protected MultiBuildItem() {
    }
}
