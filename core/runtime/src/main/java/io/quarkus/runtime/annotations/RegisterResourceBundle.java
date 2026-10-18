package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to register a resource bundle to be included in the native image.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RegisterResourceBundle.List.class)
public @interface RegisterResourceBundle {

    /**
     * The bundle name.
     */
    String bundleName();

    /**
     * The module name (optional).
     */
    String moduleName() default "";

    /**
     * The repeatable holder for {@link RegisterResourceBundle}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface List {
        /**
         * The {@link RegisterResourceBundle} instances.
         *
         * @return the instances
         */
        RegisterResourceBundle[] value();
    }
}
