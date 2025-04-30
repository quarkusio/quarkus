package io.quarkus.bootstrap.classloading;

import java.io.IOException;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.ManifestAttributes;
import io.quarkus.paths.OpenPathTree;

public class FilteredClassPathElement implements ClassPathElement {

    private final ClassPathElement delegate;
    private final Set<String> removed;
    private final Set<String> resources;

    public FilteredClassPathElement(ClassPathElement delegate, Collection<String> removed) {
        this.delegate = delegate;
        this.removed = new HashSet<>(removed);
        this.resources = new HashSet<>(delegate.getProvidedResources());
        this.resources.removeAll(this.removed);
    }

    @Override
    public ResolvedDependency getResolvedDependency() {
        return delegate.getResolvedDependency();
    }

    @Override
    public boolean isRuntime() {
        return delegate.isRuntime();
    }

    @Override
    public <T> T apply(Function<OpenPathTree, T> func) {
        return delegate.apply(func);
    }

    @Override
    public Path getRoot() {
        return delegate.getRoot();
    }

    @Override
    public ClassPathResource getResource(String name) {
        if (removed.contains(name)) {
            return null;
        }
        return delegate.getResource(name);
    }

    @Override
    public Set<String> getProvidedResources() {
        return resources;
    }

    @Override
    public boolean containsReloadableResources() {
        return delegate.containsReloadableResources();
    }

    @Override
    public ProtectionDomain getProtectionDomain() {
        return delegate.getProtectionDomain();
    }

    @Override
    public ManifestAttributes getManifestAttributes() {
        //we don't support filtering the manifest
        return delegate.getManifestAttributes();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
