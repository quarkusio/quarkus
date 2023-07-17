package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * When used on a {@link List} parameter annotated with {@link RestQuery}, RESTEasy Reactive will split the value of the
 * parameter (using the value of the annotation) and populate the list with those values.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface Separator {

    String value();
}
