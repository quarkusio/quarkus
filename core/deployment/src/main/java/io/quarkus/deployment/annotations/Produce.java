package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.builder.item.VirtualBuildItem;

/**
 * Declare that this step comes before the given symbolic marker.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ProduceRepeatable.class)
public @interface Produce {
    /**
     * The virtual build item type that this comes before.
     *
     * @return the virtual build item
     */
    Class<? extends VirtualBuildItem> value();
}
