package io.quarkus.arc.arquillian;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

import io.quarkus.arc.ComponentsProvider;

/**
 * Loads the test classes and ArC-generated classes for an Arquillian test. There's one
 * {@code DeploymentClassLoader} for each test, which is closed at the end.
 * <p>
 * The delegation model of this class loader is "child first". That is, this class loader
 * attempts to find the requested class on its own (which succeeds for the test classes and
 * ArC-generated classes) and it only delegates to the parent if it fails. This makes sure
 * that the ArC-generated classes have package-level visibility into test classes, which
 * would not be the case if the test classes were loaded by the parent.
 */
final class DeploymentClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final DeploymentDir deploymentDir;

    DeploymentClassLoader(DeploymentDir deploymentDir) throws IOException {
        super(findUrls(deploymentDir));
        this.deploymentDir = deploymentDir;
        setDefaultAssertionStatus(true);
    }

    private static URL[] findUrls(DeploymentDir deploymentDir) throws IOException {
        List<URL> result = new ArrayList<>();

        result.add(deploymentDir.appClasses.toUri().toURL());
        result.add(deploymentDir.generatedClasses.toUri().toURL());

        try (Stream<Path> stream = Files.walk(deploymentDir.appLibraries)) {
            for (Path jar : stream.filter(p -> p.toString().endsWith(".jar")).toList()) {
                result.add(jar.toUri().toURL());
            }
        }

        return result.toArray(new URL[0]);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz != null) {
                return clazz;
            }

            try {
                clazz = findClass(name);
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            } catch (ClassNotFoundException ignored) {
                return super.loadClass(name, resolve);
            }
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (("META-INF/services/" + ComponentsProvider.class.getName()).equals(name)) {
            URL url = deploymentDir.generatedServices.resolve(ComponentsProvider.class.getName()).toUri().toURL();
            return Collections.enumeration(Collections.singleton(url));
        }
        return super.getResources(name);
    }
}
