package io.quarkus.gradle.model.pom;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class DeclaredDepsResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final List<DeclaredDependency> declaredDependencies;
    private final boolean resolved;

    private DeclaredDepsResult(List<DeclaredDependency> declaredDependencies, boolean resolved) {
        this.declaredDependencies = declaredDependencies;
        this.resolved = resolved;
    }

    public static DeclaredDepsResult resolved(List<DeclaredDependency> declaredDependencies) {
        return new DeclaredDepsResult(declaredDependencies, true);
    }

    public static DeclaredDepsResult unresolved() {
        return new DeclaredDepsResult(List.of(), false);
    }

    public List<DeclaredDependency> getDeclaredDependencies() {
        return declaredDependencies;
    }

    public boolean isResolved() {
        return resolved;
    }
}
