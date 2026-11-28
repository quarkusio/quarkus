package io.quarkus.gradle.model.pom;

import java.util.Collection;

import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.UnresolvableModelException;

import io.quarkus.maven.dependency.GAV;

public interface PomResolver {

    ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException;

    /**
     * Gives Gradle-backed resolvers a chance to populate their cache with several POM lookup results at once.
     */
    default void prefetchPoms(Collection<GAV> gavs) {
    }

    /**
     * Returns whether this resolver already has a resolved or known-missing result for the given POM.
     * <p>
     * Resolvers that do not maintain an explicit cache return {@code true} so callers fall back to direct
     * {@link #resolvePom(GAV)} calls instead of trying to prefetch unsupported lookups.
     */
    default boolean hasPomResult(GAV gav) {
        return true;
    }
}
