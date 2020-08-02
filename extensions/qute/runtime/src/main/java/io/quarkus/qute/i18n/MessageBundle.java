package io.quarkus.qute.i18n;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Denotes a message bundle interface.
 * <p>
 * Each method represents a single message:
 * 
 * <pre>
 * <code>
 * &#64;MessageBundle
 * interface MyBundle {
 * 
 *     &#64;Message("Hello {name}!")
 *     String hello_world(String name);
 * }
 * </code>
 * </pre>
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface MessageBundle {

    /**
     * Constant value for {@link #locale()} indicating that the default locale of the Java Virtual Machine used to build the
     * application should be used.
     */
    String DEFAULT_LOCALE = "<<default locale>>";

    /**
     * Constant value for {@link #value()}.
     */
    String DEFAULT_NAME = "msg";

    /**
     * The name is used as a namespace in templates expressions.
     * 
     * By default, the namespace {@value #DEFAULT_NAME} is used:
     * 
     * <pre>
     * {msg:hello_world}
     * </pre>
     * 
     * If multiple bundles declare the same name the build fails.
     * 
     * @return the name
     */
    String value() default DEFAULT_NAME;

    /**
     * The value may be one of the following: {@link Message#ELEMENT_NAME}, {@link Message#HYPHENATED_ELEMENT_NAME} and
     * {@link Message#UNDERSCORED_ELEMENT_NAME}.
     * 
     * @return the default key strategy
     * @see Message#key()
     */
    String defaultKey() default Message.ELEMENT_NAME;

    /**
     * 
     * @return the locale for the default message bundle
     */
    String locale() default DEFAULT_LOCALE;
}
