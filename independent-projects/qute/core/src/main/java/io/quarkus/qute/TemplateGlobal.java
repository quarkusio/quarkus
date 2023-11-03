package io.quarkus.qute;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * <strong>IMPORTANT: This annotation only works in a fully integrated environment; such as a Quarkus application.</strong>
 * </p>
 *
 * Denotes static fields and methods that supply global variables which are accessible in every template.
 * <p>
 * If a class is annotated with {@link TemplateGlobal} then every non-void non-private static method that declares no parameters
 * and every non-private static field is considered a global variable. The name is defaulted, i.e. the name of the
 * field/method is used.
 * <p>
 * A global variable method:
 * <ul>
 * <li>must not be private,</li>
 * <li>must be static,</li>
 * <li>must not accept any parameter,</li>
 * <li>must not return {@code void}.</li>
 * </ul>
 * <p>
 * A global variable field:
 * <ul>
 * <li>must not be private,</li>
 * <li>must be static.</li>
 * </ul>
 */
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Retention(RUNTIME)
public @interface TemplateGlobal {

    /**
     * Constant value for {@link #name()} indicating that the field/method name should be used.
     */
    String ELEMENT_NAME = "<<element name>>";

    String name() default ELEMENT_NAME;

}
