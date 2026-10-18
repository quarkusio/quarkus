package io.quarkus.bootstrap.resolver.maven;

import java.util.Objects;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Collects direct dependencies that are selected and not selected optional and provided dependencies.
 */
class DirectDependencyCollector implements DependencySelector {

    private final DependencySelector delegate;
    private final ArtifactDependencyMap parent;

    DirectDependencyCollector(DependencySelector delegate, ArtifactDependencyMap parent) {
        this.delegate = Objects.requireNonNull(delegate);
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        final boolean selected = delegate.selectDependency(dependency);
        if (selected || dependency.isOptional() || JavaScopes.PROVIDED.equals(dependency.getScope())) {
            parent.putDependency(dependency);
        }
        return selected;
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        final DependencySelector derivedChildSelector = delegate.deriveChildSelector(context);
        if (derivedChildSelector.getClass() == getClass()) {
            throw new RuntimeException(getClass().getSimpleName() + " is not expected to be derived by a delegate");
        }
        return new DirectDependencyCollector(derivedChildSelector,
                parent.getApplicationDependencies().getOrCreate(context.getDependency()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;

        DirectDependencyCollector that = (DirectDependencyCollector) o;
        return parent.getDependency().equals(that.parent.getDependency())
                && delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return 31 * delegate.hashCode() + parent.hashCode();
    }
}
