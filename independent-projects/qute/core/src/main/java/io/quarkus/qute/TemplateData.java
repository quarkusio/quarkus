package io.quarkus.qute;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.quarkus.qute.TemplateData.Container;

/**
 * This annotation is used to mark a target type for which a value resolver should be automatically generated. Note that
 * non-public members, constructors, static initializers, static, synthetic and void methods are always ignored.
 * 
 * @see ValueResolver
 */
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(Container.class)
public @interface TemplateData {

    /**
     * The class a value resolver should be generated for. By default, the annotated type.
     */
    Class<?> target() default TemplateData.class;

    /**
     * The regular expressions that are used to match the members that should be ignored.
     */
    String[] ignore() default {};

    /**
     * If set to true do not automatically analyze superclasses.
     */
    boolean ignoreSuperclasses() default false;

    /**
     * If set to true include only properties: instance fields and methods without params.
     */
    boolean properties() default false;

    @Retention(RUNTIME)
    @Target(TYPE)
    @interface Container {

        TemplateData[] value();

    }

}
