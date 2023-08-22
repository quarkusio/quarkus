package io.quarkus.test.component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;

import io.quarkus.arc.ComponentsProvider;

class QuarkusComponentTestClassLoader extends ClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final Map<String, byte[]> localClasses; // generated and transformed classes
    private final File componentsProviderFile;

    public QuarkusComponentTestClassLoader(ClassLoader parent, Map<String, byte[]> localClasses,
            File componentsProviderFile) {
        super(parent);

        this.localClasses = localClasses;
        this.componentsProviderFile = Objects.requireNonNull(componentsProviderFile);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz != null) {
                return clazz;
            }

            byte[] bytecode = null;
            if (localClasses != null) {
                bytecode = localClasses.get(name);
            }
            if (bytecode == null && !mustDelegateToParent(name)) {
                String path = name.replace('.', '/') + ".class";
                try (InputStream in = getParent().getResourceAsStream(path)) {
                    if (in != null) {
                        bytecode = in.readAllBytes();
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            if (bytecode != null) {
                clazz = defineClass(name, bytecode, 0, bytecode.length);
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }

            return super.loadClass(name, resolve);
        }
    }

    private static boolean mustDelegateToParent(String name) {
        return name.startsWith("java.")
                || name.startsWith("jdk.")
                || name.startsWith("javax.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.ietf.jgss.")
                || name.startsWith("org.w3c.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.jcp.xml.")
                || name.equals("io.quarkus.dev.testing.TracingHandler");
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (componentsProviderFile != null
                && ("META-INF/services/" + ComponentsProvider.class.getName()).equals(name)) {
            return Collections.enumeration(Collections.singleton(componentsProviderFile.toURI().toURL()));
        }
        return super.getResources(name);
    }

    public static QuarkusComponentTestClassLoader inTCCL() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl instanceof QuarkusComponentTestClassLoader) {
            return (QuarkusComponentTestClassLoader) tccl;
        }
        throw new IllegalStateException("TCCL is not QuarkusComponentTestClassLoader, the `@RegisterExtension` field"
                + " of type `QuarkusComponentTestExtension` must be `static`");
    }
}
