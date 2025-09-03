package io.quarkus.cache.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import io.quarkus.cache.CachedResults;

/**
 * This is an internal qualifier and should not be used by application beans.
 *
 * @see CachedResults
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, FIELD, PARAMETER })
public @interface CachedResultsDiff {

    String value();

}
