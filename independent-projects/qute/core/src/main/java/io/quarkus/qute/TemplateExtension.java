package io.quarkus.qute;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Instructs a fully integrated environment to generate a {@link ValueResolver} for a method annotated with this annotation.
 * <p>
 * If declared on a class a value resolver is generated for every non-private static method declared on the class. Method-level
 * annotations override the behavior defined on the class. Methods that do not meet the following requirements are ignored.
 * 
 * <p>
 * A template extension method:
 * <ul>
 * <li>must not be private</li>
 * <li>must be static,</li>
 * <li>must not return {@code void}.</li>
 * </ul>
 * 
 * If there is no namespace defined the class of the first parameter that is not annotated with {@link TemplateAttribute} is
 * used to match the base object. Otherwise the namespace is used to match an expression.
 * 
 * <pre>
 * {@literal @}TemplateExtension
 * static BigDecimal discountedPrice(Item item) {
 *    // this method matches {item.discountedPrice} iff "item" resolves to an object assignable to "Item" 
 *    return item.getPrice().multiply(new BigDecimal("0.9"));
 * }
 * </pre>
 * 
 * By default, the method name is used to match the property name. However, it is possible to specify the matching name with
 * {@link #matchName()}.
 * 
 * <pre>
 * {@literal @}TemplateExtension(matchName = "discounted")
 * static BigDecimal discountedPrice(Item item) {
 *    // this method matches {item.discounted} iff "item" resolves to an object assignable to "Item" 
 *    return item.getPrice().multiply(new BigDecimal("0.9"));
 * }
 * </pre>
 * 
 * A special constant - {@link #ANY} - may be used to specify that the extension method matches any name.
 * It is also possible to match the name against a regular expression specified in {@link #matchRegex()}. In both cases, an
 * additional string method parameter must be used to pass the property name. If both {@link #matchName()} and
 * {@link #matchRegex()} are set the regular expression is used for matching.
 * 
 * <pre>
 * {@literal @}TemplateExtension(matchName = "*")
 * static String itemProperty(Item item, String name) {
 *    // this method matches {item.foo} iff "item" resolves to an object assignable to "Item"
 *    // the value of the "name" parameter is "foo"
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
     * @see NamespaceResolver#getNamespace()
     */
    String namespace() default "";

    /**
     * Used to annotated a template extension method parameter that should be obtained via
     * {@link TemplateInstance#getAttribute(String)}. The parameter type must be {@link java.lang.Object}.
     * 
     * <pre>
    * {@literal @}TemplateExtension
    * static BigDecimal discountedPrice(Item item, {@literal @}TemplateAttribute Object locale) {
    *    // this method matches {item.discountedPrice}
    *    // ... do some locale-aware formatting
    * }
     * </pre>
     */
    @Retention(RUNTIME)
    @Target(PARAMETER)
    @interface TemplateAttribute {

        /**
         * Constant value for {@link #value()} indicating that the name of the annotated parameter should be used as-is.
         */
        String PARAMETER_NAME = "<<parameter name>>";

        /**
         * 
         * @return the key used to obtain the attribute
         * @see TemplateInstance#getAttribute(String)
         */
        String value() default PARAMETER_NAME;

    }

}
