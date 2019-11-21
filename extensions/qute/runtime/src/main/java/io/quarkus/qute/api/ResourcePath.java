package io.quarkus.qute.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;

import io.quarkus.qute.runtime.QuteConfig;

/**
 * Qualifies an injected template. The {@link #value()} is used to locate the template; it represents the path relative from
 * {@code META-INF/resources/${basePath}}.
 * 
 * @see QuteConfig#basePath
 */
@Qualifier
@Retention(RUNTIME)
@Target({ FIELD, PARAMETER, METHOD })
public @interface ResourcePath {

    /**
     * @return the path relative from {@code META-INF/resources/${basePath}}, must not be an empty string
     * @see QuteConfig#basePath
     */
    @Nonbinding
    String value();

}