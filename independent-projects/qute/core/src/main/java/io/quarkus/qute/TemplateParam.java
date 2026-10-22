package io.quarkus.qute;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to bind a checked template method parameter with a template parameter declaration.
 * <p>
 * By default, the parameter name is obtained from the method parameter metadata, which requires the Java compiler
 * to be configured with the {@code -parameters} flag. This annotation allows explicit parameter naming without
 * the {@code -parameters} flag and enables different names in templates vs Java code.
 *
 * <h2>Example</h2>
 *
 * <pre>
 * <code>
 * &#64;CheckedTemplate
 * static class Templates {
 *     static native TemplateInstance hello(&#64;TemplateParam("name") String userName);
 * }
 * </code>
 * </pre>
 *
 * In this example, the template will use the variable {@code name}, while the Java method parameter is {@code userName}.
 *
 * @see CheckedTemplate
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TemplateParam {

    /**
     * The name of the template parameter.
     *
     * @return the template parameter name
     */
    String value();
}
