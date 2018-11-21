package org.jboss.shamrock.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given class can be injected as a configuration object
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ConfiguredType {
}
