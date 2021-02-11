package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.builder.item.BuildItem;

/**
 * Declare that this step comes after the given build item is produced.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Consume.List.class)
public @interface Consume {
    /**
     * The build item type that this comes after.
     *
     * @return the build item
     */
    Class<? extends BuildItem> value();

    /**
     * The repeatable holder for {@link Consume}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        /**
         * The {@link Consume} instances.
         *
         * @return the instances
         */
        Consume[] value();
    }

}
