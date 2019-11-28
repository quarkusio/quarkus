package io.quarkus.runner.classloading;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * A class path element that can be used to hide resources from another class path element.
 *
 * TODO: this will likely go away once the new class loading architecture is full implemented, this is only present
 * to emulate the existing class loader functionality
 */
public class FilteringClassPathElement implements ClassPathElement {

    private final ClassPathElement delegate;
    private final Set<String> filter;

    public FilteringClassPathElement(ClassPathElement delegate, Set<String> filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public ClassPathResource getResource(String name) {
        if (filter.contains(name)) {
            return null;
        }
        ClassPathResource res = delegate.getResource(name);
        if (res == null) {
            return null;
        }
        return new ClassPathResource() {
            @Override
            public ClassPathElement getContainingElement() {
                return FilteringClassPathElement.this;
            }

            @Override
            public String getPath() {
                return res.getPath();
            }

            @Override
            public URL getUrl() {
                return res.getUrl();
            }

            @Override
            public byte[] getData() {
                return res.getData();
            }
        };
    }

    @Override
    public Set<String> getProvidedResources() {
        Set<String> providedResources = new HashSet<>(delegate.getProvidedResources());
        providedResources.removeAll(filter);
        return providedResources;
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        return delegate.getProtectionDomain(classLoader);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
