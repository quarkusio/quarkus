package io.quarkus.bootstrap.classloading;

import java.io.IOException;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;

public class FilteredClassPathElement implements ClassPathElement {

    final ClassPathElement delegate;
    final Set<String> removed;

    public FilteredClassPathElement(ClassPathElement delegate, Collection<String> removed) {
        this.delegate = delegate;
        this.removed = new HashSet<>(removed);
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
        Set<String> ret = new HashSet<>(delegate.getProvidedResources());
        ret.removeAll(removed);
        return ret;
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        return delegate.getProtectionDomain(classLoader);
    }

    @Override
    public Manifest getManifest() {
        //we don't support filtering the manifest
        return delegate.getManifest();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
