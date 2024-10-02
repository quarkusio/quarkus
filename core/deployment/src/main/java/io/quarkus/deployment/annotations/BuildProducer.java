package io.quarkus.deployment.annotations;

import java.util.Collection;

import io.quarkus.builder.item.BuildItem;

/**
 * An interface that can be injected to produce {@link BuildItem} instances
 *
 * This can be injected into either a field or method parameter. To produce
 * a {@link BuildItem} simply call the {@link #produce(BuildItem)} method
 * with the instance.
 *
 *
 * @param <T> The type of build item to produce
 */
public interface BuildProducer<T extends BuildItem> {

    void produce(T item);

    default void produce(Collection<T> items) {
        items.forEach(this::produce);
    }
}
