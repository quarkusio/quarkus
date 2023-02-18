package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that allows Quarkus to know that a Collection or Map passed to a recorder (either directly or as part a
 * field of bean type object)
 * is only read via that recorder and never altered.
 * Quarkus uses this information in order to create optimized Collections and Maps of the recorded values.
 *
 * Extensions writers should NOT rely on Quarkus to always create an immutable Collection or Map even if the annotation is used,
 * and thus recorder logic should not be dependent on the type of Collection or Map passed on by Quarkus, as Quarkus only makes
 * a best-effort attempt to do create such immutable objects but cannot guarantee it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ReadOnly {
}
