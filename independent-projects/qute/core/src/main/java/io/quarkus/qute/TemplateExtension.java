package io.quarkus.qute;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A value resolver is automatically generated for a template extension method.
 * <p>
 * The method must be static, must not return {@code void} and must accept at least one parameter. The class of the first
 * parameter is used to match the base object.
 * <p>
 * By default, the method name is used to match the property name. However, it is possible to specify the matching name with
 * {@link #matchName()}. A special constant - {@link #ANY} - may be used to specify that the extension method matches any name.
 * In that case, the method must declare at least two parameters and the second parameter must be a string.
 * 
 * <pre>
 * {@literal @}TemplateExtension
 * static BigDecimal discountedPrice(Item item) {
 *    // this method matches {item.discountedPrice}
 *    return item.getPrice().multiply(new BigDecimal("0.9"));
 * }
 * </pre>
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface TemplateExtension {

    static final String ANY = "*";

    String matchName() default "";

}
