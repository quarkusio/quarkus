package io.quarkus.test.component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;

import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.ResourceReferenceProvider;

class QuarkusComponentTestClassLoader extends ClassLoader {

    private final File componentsProviderFile;
    private final File resourceReferenceProviderFile;

    public QuarkusComponentTestClassLoader(ClassLoader parent, File componentsProviderFile,
            File resourceReferenceProviderFile) {
        super(parent);
        this.componentsProviderFile = Objects.requireNonNull(componentsProviderFile);
        this.resourceReferenceProviderFile = resourceReferenceProviderFile;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (("META-INF/services/" + ComponentsProvider.class.getName()).equals(name)) {
            // return URL that points to the correct components provider
            return Collections.enumeration(Collections.singleton(componentsProviderFile.toURI().toURL()));
        } else if (resourceReferenceProviderFile != null
                && ("META-INF/services/" + ResourceReferenceProvider.class.getName()).equals(name)) {
            return Collections.enumeration(Collections.singleton(resourceReferenceProviderFile.toURI().toURL()));
        }
        return super.getResources(name);
    }

}
