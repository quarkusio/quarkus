package org.jboss.resteasy.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the MIME type of each SSE element in the annotated stream.
 *
 * @deprecated replaced by {@link RestStreamElementType}
 */
@Inherited
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Deprecated
public @interface RestSseElementType {
    String value();
}
