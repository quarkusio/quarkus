package io.quarkus.gradle.model.pom;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Serializable outcome of reading one module's declared dependencies.
 * <p>
 * A resolved result with an empty list is distinct from an unresolved result. This distinction lets model generation
 * avoid treating an unavailable effective POM as a POM that declares no dependencies. The type is an internal Gradle
 * model value rather than application DSL.
 */
public class DeclaredDepsResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<DeclaredDependency> declaredDependencies;
    private final boolean resolved;

    private DeclaredDepsResult(List<DeclaredDependency> declaredDependencies, boolean resolved) {
        this.declaredDependencies = declaredDependencies;
        this.resolved = resolved;
    }

    /**
     * Creates a successful result.
     *
     * @param declaredDependencies dependencies in the order supplied by the effective Maven model
     * @return resolved result
     */
    public static DeclaredDepsResult resolved(List<DeclaredDependency> declaredDependencies) {
        return new DeclaredDepsResult(declaredDependencies, true);
    }

    /**
     * Creates a result indicating that the effective model could not be built.
     *
     * @return unresolved result with an empty dependency list
     */
    public static DeclaredDepsResult unresolved() {
        return new DeclaredDepsResult(List.of(), false);
    }

    /**
     * @return declared dependencies, or an empty list for an unresolved result
     */
    public List<DeclaredDependency> getDeclaredDependencies() {
        return declaredDependencies;
    }

    /**
     * @return {@code true} if an effective model was built, including when it declared no dependencies
     */
    public boolean isResolved() {
        return resolved;
    }
}
