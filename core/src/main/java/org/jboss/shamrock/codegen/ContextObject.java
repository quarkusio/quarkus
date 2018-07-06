package org.jboss.shamrock.codegen;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation that is used to specify that the annotated item should be stored in or retrieved from the context
 * data map.
 *
 * If this annotation is applied to a method then the return value of the method is stored in the context map under
 * the named key.
 *
 * If this method is applied to a parameter then the value of that parameter is retrieved from the context map
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextObject {
    String value();
}
