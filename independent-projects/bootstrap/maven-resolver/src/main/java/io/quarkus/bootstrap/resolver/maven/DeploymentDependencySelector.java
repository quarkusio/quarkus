package io.quarkus.bootstrap.resolver.maven;

import java.util.Objects;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

final class DeploymentDependencySelector implements DependencySelector {

    static DependencySelector ensureDeploymentDependencySelector(DependencySelector original) {
        return original.getClass() == DeploymentDependencySelector.class ? original
                : new DeploymentDependencySelector(original);
    }

    private final DependencySelector delegate;

    DeploymentDependencySelector(DependencySelector delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        return !dependency.isOptional() && delegate.selectDependency(dependency);
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        final DependencySelector newDelegate = delegate.deriveChildSelector(context);
        return newDelegate == null ? null : ensureDeploymentDependencySelector(newDelegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeploymentDependencySelector other = (DeploymentDependencySelector) obj;
        return Objects.equals(delegate, other.delegate);
    }
}
