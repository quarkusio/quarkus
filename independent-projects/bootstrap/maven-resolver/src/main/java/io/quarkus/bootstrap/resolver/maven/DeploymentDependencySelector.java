package io.quarkus.bootstrap.resolver.maven;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

class DeploymentDependencySelector implements DependencySelector {

    private final DependencySelector delegate;

    public DeploymentDependencySelector(DependencySelector delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        if (dependency.isOptional()) {
            return false;
        }
        return delegate.selectDependency(dependency);
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        final DependencySelector newDelegate = delegate.deriveChildSelector(context);
        return newDelegate == null ? null : new DeploymentDependencySelector(newDelegate);
    }
}
