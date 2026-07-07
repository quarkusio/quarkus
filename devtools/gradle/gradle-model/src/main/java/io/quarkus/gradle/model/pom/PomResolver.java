package io.quarkus.gradle.model.pom;

import java.util.Collection;

import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.UnresolvableModelException;

import io.quarkus.maven.dependency.GAV;

/**
 * Supplies Maven POM model sources while effective models are built from Gradle-selected modules.
 * <p>
 * Implementations may be Gradle-backed or local-only. The prefetch/cache protocol lets callers batch lookups discovered
 * during Maven model building, including parent POMs and imported BOMs, before retrying the effective-model build.
 */
public interface PomResolver {

    /**
     * Returns the model source for {@code gav}.
     *
     * @param gav POM coordinates to resolve
     * @return Maven model source
     * @throws UnresolvableModelException when the POM is unavailable
     */
    ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException;

    /**
     * Gives resolvers a chance to populate resolved or known-missing cache entries for several POMs at once.
     * <p>
     * The default is a no-op for resolvers that support only direct lookup.
     *
     * @param gavs POM coordinates to prefetch
     */
    default void prefetchPoms(Collection<GAV> gavs) {
    }

    /**
     * Returns whether this resolver already has a resolved or known-missing result for the given POM.
     * <p>
     * Resolvers that do not maintain an explicit cache return {@code true} so callers fall back to direct
     * {@link #resolvePom(GAV)} calls instead of trying to prefetch unsupported lookups.
     *
     * @param gav coordinates to query
     * @return whether a lookup result is already available
     */
    default boolean hasPomResult(GAV gav) {
        return true;
    }
}
