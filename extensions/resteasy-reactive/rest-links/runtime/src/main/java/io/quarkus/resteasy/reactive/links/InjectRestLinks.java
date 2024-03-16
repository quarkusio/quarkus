package io.quarkus.resteasy.reactive.links;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inject web links into the response HTTP headers with the "Link" header field.
 * Only the response of the REST methods annotated with {@link RestLink} will include the "Link" headers.
 * <p>
 * The InjectRestLinks annotation can be used at either class or method levels.
 * <p>
 *
 * @see <a href="https://www.rfc-editor.org/info/rfc5988">RFC 5988 Web Linking Standard</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface InjectRestLinks {

    /**
     * Find all the types available in {@link RestLinkType}.
     *
     * @return what types of links will be injected.
     */
    RestLinkType value() default RestLinkType.TYPE;
}
