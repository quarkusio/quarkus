package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to force a class to be registered for reflection in native image mode
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterForReflection {

    /**
     * If the methods should be registered
     */
    boolean methods() default true;

    /**
     * If the fields should be registered
     */
    boolean fields() default true;

    /**
     * Alternative classes that should actually be registered for reflection instead of the current class.
     *
     * This allows for classes in 3rd party libraries to be registered without modification or writing an
     * extension. If this is set then the class it is placed on is not registered for reflection, so this should
     * generally just be placed on an empty class that is not otherwise used.
     */
    Class<?>[] targets() default {};

    /**
     * If this is true then not only will this class be registered, but an attempt will also be made
     * to register all classes this class depends on. This includes:
     *
     * <ul>
     *     <li>Superclasses</li>
     *     <li>Field types</li>
     *     <li>Method return types</li>
     *     <li>Method parameter types</li>
     *     <li>Type parameters of the above (e.g. a field of type List&lt;Foo&gt; would result in Foo being registered)</li>
     * </ul>
     *
     * This process is applied recursively, so every type registered in this way will also have it's dependencies registered.
     *
     * This should generally only be used for serialization, where a serialization library needs access to all the
     * details of an object.
     *
     */
    boolean registerDependencies() default false;
}
