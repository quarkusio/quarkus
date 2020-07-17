package io.quarkus.qute.i18n;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identifies a message bundle method.
 * <p>
 * Each method of a message bundle interface annotated with {@link MessageBundle} must be annotated with this annotation.
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
     * 
     * @return the key
     */
    String key() default DEFAULT_NAME;

    /**
     * 
     * @return the message template
     */
    String value();

}
