package io.quarkus.resteasy.reactive.links;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a Web link to be incorporated into the HTTP response.
 * Only the response of methods or classes annotated with {@link InjectRestLinks} will include the "Link" headers.
 * <p>
 * The RestLink annotation can be used at method level.
 * <p>
 *
 * @see <a href="https://www.rfc-editor.org/info/rfc5988">RFC 5988 Web Linking Standard</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RestLink {

    /**
     * If not set, it will default to the method name.
     *
     * @return the link relation.
     */
    String rel() default "";

    /**
     * Intended for labelling the link with a human-readable identifier.
     *
     * @return the link title.
     */
    String title() default "";

    /**
     * Hint to indicate the media type expected when dereferencing the target resource.
     *
     * @return the link expected media type.
     */
    String type() default "";

    /**
     * Declares a link for the given type of resources.
     * If not set, it will default to the returning type of the annotated method.
     *
     * @return the type of returning method.
     */
    Class<?> entityType() default Object.class;
}
