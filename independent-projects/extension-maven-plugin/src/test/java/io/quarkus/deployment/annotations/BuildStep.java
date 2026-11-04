package io.quarkus.deployment.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Stand-in for the real annotation, which lives in {@code quarkus-core-deployment}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface BuildStep {
}
