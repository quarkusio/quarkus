package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on fields of {@link MultipartForm} POJOs to designate the media type the corresponding body part maps to.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PartType {
    String value();
}
