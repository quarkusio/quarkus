package io.quarkus.qute;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * <strong>IMPORTANT: This annotation only works in a fully integrated environment; such as a Quarkus application.</strong>
 * </p>
 *
 * This annotation can be used to specify the contents for a type-safe template, i.e. for a method of a class annotated with
 * {@link CheckedTemplate} or a Java record that implements {@link TemplateInstance}.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface TemplateContents {

    /**
     * The contents.
     */
    String value();

    /**
     * The suffix is used to determine the content type of a template. This is useful when working with template variants.
     * <p>
     * By default, the {@code txt} value, which corresponds to {@code text/plain}, is used. For the {@code text/html} content
     * type the {@code html} suffix should be used.
     */
    String suffix() default "txt";
}
