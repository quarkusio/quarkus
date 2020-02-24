package io.quarkus.deployment.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.builder.item.BuildItem;

/**
 * Declare that this step comes before the given build items are consumed, but using {@linkplain Weak weak semantics}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ProduceWeak.List.class)
public @interface ProduceWeak {
    /**
     * The build item type whose consumption is preceded by this step.
     *
     * @return the build item
     */
    Class<? extends BuildItem> value();

    /**
     * The repeatable holder for {@link ProduceWeak}.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        /**
         * The {@link ProduceWeak} instances.
         *
         * @return the instances
         */
        ProduceWeak[] value();
    }
}