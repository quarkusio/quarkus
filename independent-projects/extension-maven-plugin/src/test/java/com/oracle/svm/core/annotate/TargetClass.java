package com.oracle.svm.core.annotate;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Stand-in for the real GraalVM annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetClass {
}
