package io.quarkus.qute;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A value resolver is automatically generated for a method annotated with this annotation. If declared on a class a value
 * resolver is generated for every non-private static method declared on the class. Method-level annotations override the
 * behavior defined on the class.
 * <p>
 * Methods that do not meet the following requirements are ignored.
 * <p>
 * A template extension method:
 * <ul>
 * <li>must be static,</li>
 * <li>must not return {@code void},</li>
 * <li>must accept at least one parameter, unless the namespace is specified.</li>
 * </ul>
 * The class of the first parameter is used to match the base object unless the namespace is specified. In such case, the
 * namespace is used to match an expression.
 * <p>
 * By default, the method name is used to match the property name. However, it is possible to specify the matching name with
 * {@link #matchName()}. A special constant - {@link #ANY} - may be used to specify that the extension method matches any name.
 * It is also possible to match the name against a regular expression specified in {@link #matchRegex()}. In both cases, a
 * string method parameter is used to pass the property name.
 * <p>
 * If both {@link #matchName()} and {@link #matchRegex()} are set the regular expression is used for matching.
 * <p>
 * If a namespace is specified the method must declare at least one parameter and the first parameter must be a string. If no
 * namespace is specified the method must declare at least two parameters and the second parameter must be a string.
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

    /**
     * Constant value for {@link #matchName()} indicating that the method name should be used.
     */
    String METHOD_NAME = "<<method name>>";

    /**
     * Constant value for {@link #matchName()} indicating that any name matches.
     */
    String ANY = "*";

    /**
     * Constant value for {{@link #priority()}.
     */
    int DEFAULT_PRIORITY = 5;

    /**
     * 
     * @return the name is used to match the property name
     */
    String matchName() default METHOD_NAME;

    /**
     * 
     * @return the regex is used to match the property name
     */
    String matchRegex() default "";

    /**
     * 
     * @return the priority used by the generated value resolver
     */
    int priority() default DEFAULT_PRIORITY;

    /**
     * If not empty a namespace resolver is generated instead.
     * <p>
     * Template extension methods that share the same namespace and are declared on the same class are grouped in one resolver
     * and ordered by {@link #priority()}. The first matching extension method is used to resolve an expression. Template
     * extension methods declared on different classes cannot share the same namespace.
     * 
     * @return the namespace
     */
    String namespace() default "";

}
