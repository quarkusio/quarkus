package io.quarkus.bootstrap.resolver.maven;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Derives {@link DirectDependencyCollector}
 */
class DirectDependencyCollectorFactory implements DependencySelector {

    private final DependencySelector delegate;
    private final ApplicationDependencyMap appDepMap;

    public DirectDependencyCollectorFactory(DependencySelector delegate, ApplicationDependencyMap appDepMap) {
        this.delegate = delegate;
        this.appDepMap = appDepMap;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        return new DirectDependencyCollector(delegate.deriveChildSelector(context),
                appDepMap
                        .getOrCreate(context.getDependency() == null ? new Dependency(context.getArtifact(), JavaScopes.COMPILE)
                                : context.getDependency()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        DirectDependencyCollectorFactory that = (DirectDependencyCollectorFactory) o;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode() {
        return 31 * delegate.hashCode();
    }
}
