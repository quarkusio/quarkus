package io.quarkus.qute.i18n;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies a message bundle method that represents a single message of the enclosing bundle.
 * <p>
 * The value of the {@link #key()} can be used to reference a message in a template. It is possible to specify an explicit key
 * or a strategy to extract the default key. The default strategy is defined by the enclosing
 * {@link MessageBundle#defaultKey()}.
 * <p>
 * The {@link #value()} defines the template of a message. The method parameters can be used in this template. All the message
 * templates are validated at build time. If there is no template defined the template from a localized file is taken. In case
 * the value is not provided at all the build fails.
 * <p>
 * Note that any method declared on a message bundle interface is consireded a message bundle method. If not annotated with this
 * annotation then the defaulted values are used for the key and template.
 * <p>
 * All message bundle methods must return {@link String}. If a message bundle method does not return string then the build
 * fails.
 *
 * <h2>Enums</h2>
 * There is a convenient way to localize enums.
 * <p>
 * If there is a message bundle method that accepts a single parameter of an enum type and has no message template defined then
 * it
 * receives a generated template:
 *
 * <pre>
 * {#when enumParamName}
 *     {#is CONSTANT1}{msg:methodName_CONSTANT1}
 *     {#is CONSTANT2}{msg:methodName_CONSTANT2}
 * {/when}
 * </pre>
 *
 * Furthermore, a special message method is generated for each enum constant. Finally, each localized file must contain keys and
 * values for all constant message keys:
 *
 * <pre>
 * methodName_CONSTANT1=Value 1
 * methodName_CONSTANT2=Value 2
 * </pre>
 *
 * In a template, an enum constant can be localized with a message bundle method {@code msg:methodName(enumConstant)}.
 *
 * @see MessageBundle
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Message {

    /**
     * Constant value for {@link #key()} indicating that the default strategy specified by {@link MessageBundle#defaultKey()}
     * should be used.
     */
    String DEFAULT_NAME = "<<default>>";

    /**
     * Constant value for {@link #key()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Constant value for {@link #value()} indicating that message template value specified in a localized file
     * should be used. If localized file fails to provide value, an exception is thrown and the build fails.
     */
    String DEFAULT_VALUE = "<<default value>>";

    /**
     * Constant value for {@link #key()} indicating that the annotated element's name should be de-camel-cased and
     * hyphenated, and then used.
     */
    String HYPHENATED_ELEMENT_NAME = "<<hyphenated element name>>";

    /**
     * Constant value for{@link #key()} indicating that the annotated element's name should be de-camel-cased and parts
     * separated by underscores, and then used.
     */
    String UNDERSCORED_ELEMENT_NAME = "<<underscored element name>>";

    /**
     * The key of a message.
     *
     * @return the message key
     * @see MessageBundle#defaultKey()
     */
    String key() default DEFAULT_NAME;

    /**
     * This value has higher priority over a message template specified in a localized file, and it's
     * considered a good practice to specify it. In case the value is not provided and there is no
     * match in the localized file too, the build fails.
     * <p>
     * There is a convenient way to localize enums. See the javadoc of {@link Message}.
     *
     * @return the message template
     */
    String value() default DEFAULT_VALUE;

}
