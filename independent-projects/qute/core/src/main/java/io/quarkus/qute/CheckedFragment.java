package io.quarkus.qute;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a method that represents a fragment of a type-safe template.
 * <p>
 * The name of the fragment is derived from the annotated method name. The part before the last occurence of a dollar sign
 * {@code $} is
 * the method name of the related type-safe template. The part after the last occurence of a dollar sign is the fragment
 * identifier - the strategy defined by the relevant {@link CheckedTemplate#defaultName()} is used.
 * <p>
 * Parameters of the annotated method are validated. The required names and types are derived from the relevant fragment
 * template.
 *
 * <pre>
 * &#64;CheckedTemplate
 * class Templates {
 *
 *     // defines a type-safe template
 *     static native TemplateInstance items(List&#60;Item&#62; items);
 *
 *     // defines a fragment of Templates#items() with identifier "item"
 *     &#64;CheckedFragment
 *     static native TemplateInstance items$item(Item item);
 * }
 * </pre>
 *
 * @see CheckedTemplate
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CheckedFragment {

}
