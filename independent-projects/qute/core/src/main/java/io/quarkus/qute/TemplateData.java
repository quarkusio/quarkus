package io.quarkus.qute;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.quarkus.qute.TemplateData.Container;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark a target type for which a value resolver should be automatically generated. Note that
 * non-public members, constructors, static initializers, static, synthetic and void methods are always ignored.
 * <p>
 * If the {@link #namespace()} is set to a non-empty value then a namespace resolver is automatically generated to access static
 * fields and methos of the target class.
 * 
 * @see ValueResolver
 * @see NamespaceResolver
 */
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(Container.class)
public @interface TemplateData {

    /**
     * Constant value for {@link #key()} indicating that the annotated element's name should be used as-is.
     */
    String UNDERSCORED_FQCN = "<<undescored fqcn>>";

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

    /**
     * If set to a non-empty value then a namespace resolver is automatically generated to access static fields and methos of
     * the target class.
     * <p>
     * By default, the namespace is the FQCN of the target class where dots and dollar signs are replaced by underscores, for
     * example the namespace for a class with name {@code org.acme.Foo} is {@code org_acme_Foo}.
     * <p>
     * Note that a namespace can only consist of alphanumeric characters and underscores.
     */
    String namespace() default UNDERSCORED_FQCN;

    @Retention(RUNTIME)
    @Target(TYPE)
    @interface Container {

        TemplateData[] value();

    }

}
