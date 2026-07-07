package io.quarkus.gradle.model.pom;

/**
 * Controls whether application-model dependencies are enriched with declarations from Maven POMs.
 * <p>
 * Enrichment supplements Gradle's resolved graph with Maven scope, optionality, and direct-dependency information; it
 * does not replace Gradle's variant selection, constraints, platforms, or conflict resolution.
 */
public enum DeclaredDependencyEnrichmentMode {
    /** Build the model solely from Gradle resolution results. */
    NONE,
    /** Build effective models for external modules selected in the Gradle application and deployment classpaths. */
    SELECTED_MODULE_POMS
}
