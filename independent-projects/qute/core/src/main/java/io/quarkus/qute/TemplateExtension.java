package io.quarkus.qute;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A value resolver is automatically generated for a method annotated with this annotation. If declared on a class a value
 * resolver is generated for every non-private static method declared on the class. Methods that do not meet the following
 * requirements are ignored.
 * <p>
 * A template extension method:
 * <ul>
 * <li>must be static,</li>
 * <li>must not return {@code void},</li>
 * <li>must accept at least one parameter.</li>
 * <p>
 * The class of the first parameter is used to match the base object.
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
@Target({ METHOD, TYPE })
public @interface TemplateExtension {

    static final String ANY = "*";

    String matchName() default "";

}
