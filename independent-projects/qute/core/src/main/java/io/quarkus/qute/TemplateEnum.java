package io.quarkus.qute;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * <strong>IMPORTANT: This annotation only works in a fully integrated environment; such as a Quarkus application.</strong>
 * </p>
 *
 * This annotation is functionally equivalent to {@code @TemplateData(namespace = TemplateData.SIMPLENAME)}, i.e. a namespace
 * resolver is automatically generated for the target enum. The simple name of the target enum is used as the namespace. The
 * generated namespace resolver can be used to access enum constants, static methods, etc.
 * <p>
 * If an enum also declares the {@link TemplateData} annotation or is specified by any {@link TemplateData#target()} then the
 * {@link TemplateEnum} annotation is ignored.
 * <p>
 * {@link TemplateEnum} declared on non-enum classes is ignored.
 *
 * @see TemplateData
 * @see NamespaceResolver
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface TemplateEnum {

}
