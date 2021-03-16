package io.quarkus.qute.i18n;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to bind a message bundle method parameter with a template parameter declaration.
 * <p>
 * By default, the parameter element's name is used as-is.
 * 
 * <pre>
 * <code>
 * &#64;MessageBundle
 * interface MyBundle {
 * 
 *     &#64;Message("Hello {name}!")
 *     String hello_world(&#64;MessageParam("name") String foo);
 * }
 * </code>
 * </pre>
 * 
 * @see Message
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface MessageParam {

    /**
     * 
     * @return the name of the parameter declaration
     */
    String value() default Message.ELEMENT_NAME;

}
