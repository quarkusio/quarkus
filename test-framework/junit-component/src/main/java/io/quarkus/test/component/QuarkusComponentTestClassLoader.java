package io.quarkus.test.component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.logging.Logger;

/**
 * This class loader is used to load the test class. It's also set as TCCL when a component test is run.
 */
public class QuarkusComponentTestClassLoader extends ClassLoader {

    private static final Logger LOG = Logger.getLogger(QuarkusComponentTestClassLoader.class);

    static {
        ClassLoader.registerAsParallelCapable();
    }

    static final ConcurrentMap<String, Bytecode> BYTECODE_CACHE = new ConcurrentHashMap<>();

    private static final Set<String> PARENT_CL_CLASSES = Set.of(
            "io.quarkus.test.component.QuarkusComponentTestClassLoader",
            "io.quarkus.dev.testing.TracingHandler");

    private final String name;

    private final BuildResult buildResult;

    public QuarkusComponentTestClassLoader(ClassLoader parent, String name, BuildResult buildResult) {
        super(parent);
        this.name = name;
        this.buildResult = buildResult;
    }

    @Override
    public String getName() {
        return "QuarkusComponentTestClassLoader: " + name;
    }

    public Map<String, Set<String>> getConfigMappings() {
        return buildResult.configMappings();
    }

    public Map<String, String[]> getInterceptorMethods() {
        return buildResult.interceptorMethods();
    }

    public Throwable getBuildFailure() {
        return buildResult.failure();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz != null) {
                return clazz;
            }
            byte[] bytecode = null;
            if (buildResult.generatedClasses() != null) {
                bytecode = buildResult.generatedClasses().get(name);
                if (bytecode != null) {
                    LOG.debugf("Use generated/transformed class for %s", name);
                }
            }
            if (bytecode == null && !mustDelegateToParent(name)) {
                bytecode = BYTECODE_CACHE.computeIfAbsent(name, this::loadBytecode).value();
            }
            if (bytecode != null) {
                LOG.debugf("Define class %s", name);
                clazz = defineClass(name, bytecode, 0, bytecode.length);
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
            return super.loadClass(name, resolve);
        }
    }

    private Bytecode loadBytecode(String name) {
        byte[] bytecode = null;
        String path = name.replace('.', '/') + ".class";
        try (InputStream in = getParent().getResourceAsStream(path)) {
            if (in != null) {
                LOG.debugf("Loading class %s", name);
                bytecode = in.readAllBytes();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Bytecode(bytecode);
    }

    public static boolean mustDelegateToParent(String name) {
        return name.startsWith("java.")
                || name.startsWith("jdk.")
                || name.startsWith("javax.")
                || name.startsWith("jakarta.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.w3c.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.junit.")
                || name.startsWith("org.mockito.")
                || name.startsWith("org.jboss.logging")
                || name.startsWith("org.jboss.logmanager")
                || name.startsWith("org.slf4j")
                || name.startsWith("org.jacoco")
                || PARENT_CL_CLASSES.contains(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        LOG.debugf("Get resource: %s", name);
        if (("META-INF/services/io.quarkus.arc.ComponentsProvider").equals(name)) {
            // return URL that points to the correct components provider
            File tempFile = File.createTempFile(this.name + "_ComponentsProvider", null);
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), buildResult.componentsProvider());
            LOG.debugf("ComponentsProvider tmp file written: %s", tempFile);
            return Collections.enumeration(List.of(tempFile.toURI().toURL()));
        }
        return super.getResources(name);
    }

    @Override
    public String toString() {
        return getName();
    }

    record Bytecode(byte[] value) {

    }

}
