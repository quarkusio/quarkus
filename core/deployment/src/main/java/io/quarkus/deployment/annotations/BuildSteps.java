package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.BooleanSupplier;

/**
 * Applies configuration to all build steps defined in the same class.
 * <p>
 * This annotation is applied at the class level, and will result in conditions ({@link #onlyIf()}/{@link #onlyIfNot()})
 * being prepended to all build steps defined in that class.
 * <p>
 * This is mainly useful for "enabled"-type conditions, where all steps in a class should be enabled/disabled
 * based on a single condition, for example based on configuration properties.
 *
 * @see BuildStep
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BuildSteps {

    /**
     * Only include build steps defined in this class if the given supplier class(es) return {@code true}.
     *
     * @return the supplier class array
     */
    Class<? extends BooleanSupplier>[] onlyIf() default {};

    /**
     * Only include build steps defined in this class if the given supplier class(es) return {@code false}.
     *
     * @return the supplier class array
     */
    Class<? extends BooleanSupplier>[] onlyIfNot() default {};
}
