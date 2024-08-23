package io.quarkus.qute;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.quarkus.qute.Locate.Locates;

/**
 * <p>
 * <strong>IMPORTANT: This annotation only works in a fully integrated environment; such as a Quarkus application.</strong>
 * </p>
 *
 * A custom {@link TemplateLocator}s are not available during the build time, therefore {@link Template} located by
 * the locator must disable its validation by annotating the {@link TemplateLocator} with this annotation. If
 * {@link TemplateLocator} locates {@link Template} and fails to declare the fact this way, an exception is thrown and the build
 * fails.
 *
 * An example:
 *
 * <pre>
 * &#64;Locate("/my/custom/location")
 * public class MyCustomLocator implements TemplateLocator {
 *
 *     &#64;Override
 *     public Optional<TemplateLocation> locate(String s) {
 *         return Optional.empty();
 *     }
 *
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
@Repeatable(Locates.class)
public @interface Locate {

    /**
     * @return regex pattern matching all the {@link Template#getId()}s located by the {@link TemplateLocator}
     */
    String value();

    /**
     * Enables repeating of {@link Locate}.
     */
    @Target(ElementType.TYPE)
    @Retention(RUNTIME)
    @interface Locates {

        Locate[] value();

    }
}
