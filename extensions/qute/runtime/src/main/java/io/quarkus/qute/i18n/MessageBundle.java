package io.quarkus.qute.i18n;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;

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
     * Constant value for {@link #locale()} indicating that the default locale specified via the
     * {@code quarkus.default-locale} config property should be used.
     */
    String DEFAULT_LOCALE = "<<default locale>>";

    /**
     * Constant value for {@link #value()}.
     */
    String DEFAULT_NAME = "msg";

    /**
     * Constant value for {@link #value()} indicating that the name should be defaulted.
     * <p>
     * For a top-level class the {@value #DEFAULT_NAME} is used.
     * <p>
     * For a nested class the name consists of the simple names of all enclosing classes in the hierarchy (top-level
     * class goes first), followed by the simple name of the message bundle interface. Names are separated by
     * underscores. For example, the name of the following message bundle will be defaulted to {@code Controller_index}
     * and it could be used in a template via <code>{Controller_index:hello(name)}</code>:
     *
     * <pre>
     * <code>
     * class Controller {
     *
     *    &#64;MessageBundle
     *    interface index {
     *
     *       &#64;Message("Hello {name}!")
     *       String hello(String name);
     *    }
     * }
     * </code>
     * </pre>
     */
    String DEFAULTED_NAME = "<<defaulted name>>";

    /**
     * The name is used as a namespace in templates expressions - <code>{msg:hello_world}</code>, and as a part of the
     * name of a message bundle localized file - <code>msg_de.properties</code>.
     * <p>
     * If multiple bundles declare the same name then the build fails.
     *
     * @return the name of the bundle
     */
    String value() default DEFAULTED_NAME;

    /**
     * The value may be one of the following: {@link Message#ELEMENT_NAME}, {@link Message#HYPHENATED_ELEMENT_NAME} and
     * {@link Message#UNDERSCORED_ELEMENT_NAME}.
     *
     * @return the default key strategy
     *
     * @see Message#key()
     */
    String defaultKey() default Message.ELEMENT_NAME;

    /**
     * The language tag (IETF) of the default locale.
     *
     * @return the locale for the default message bundle
     *
     * @see Locale#forLanguageTag(String)
     */
    String locale() default DEFAULT_LOCALE;
}
