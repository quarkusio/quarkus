package io.quarkus.flyway.runtime.graal;

import org.flywaydb.core.internal.FlywayTeamsObjectResolver;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Remove this class as it's not supposed to be used
 */
@Delete
@TargetClass(FlywayTeamsObjectResolver.class)
public final class FlywayTeamsObjectResolverSubstitutions {
}
