package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to force an interface (including its super interfaces) to be registered for dynamic proxy
 * generation in native image mode.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(RegisterForProxy.List.class)
public @interface RegisterForProxy {

    /**
     * Alternative interfaces that should actually be registered for dynamic proxy generation instead of the current interface.
     * This allows for interfaces in 3rd party libraries to be registered without modification or writing an
     * extension. If this is set then the interface it is placed on is not registered for dynamic proxy generation, so this
     * should generally just be placed on an empty interface that is not otherwise used.
     */
    Class<?>[] targets() default {};

    /**
     * The repeatable holder for {@link RegisterForProxy}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface List {
        /**
         * The {@link RegisterForProxy} instances.
         *
         * @return the instances
         */
        RegisterForProxy[] value();
    }
}