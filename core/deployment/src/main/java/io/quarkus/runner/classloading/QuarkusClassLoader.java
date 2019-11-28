package io.quarkus.runner.classloading;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.QuarkusClassWriter;

/**
 * The ClassLoader used for non production Quarkus applications (i.e. dev and test mode). Production
 * applications use a flat classpath so just use the system class loader.
 *
 *
 */
public class QuarkusClassLoader extends ClassLoader implements Closeable {

    private static final Logger log = Logger.getLogger(QuarkusClassLoader.class);

    static {
        registerAsParallelCapable();
    }

    private final String name;
    private final List<ClassPathElement> elements;
    private final Map<String, ClassPathElement[]> loadableResources;
    private final ConcurrentMap<ClassPathElement, ProtectionDomain> protectionDomains = new ConcurrentHashMap<>();
    private final Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers;
    private final Set<String> bannedResources;
    private final ClassLoader parent;
    private final boolean parentFirst;

    private QuarkusClassLoader(
            String name, List<ClassPathElement> elements,
            Map<String, ClassPathElement[]> loadableResources,
            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers,
            Set<String> bannedResources,
            ClassLoader parent,
            boolean parentFirst) {
        this.name = name;
        this.elements = elements;
        this.loadableResources = loadableResources;
        this.bytecodeTransformers = bytecodeTransformers;
        this.bannedResources = bannedResources;
        this.parent = parent;
        this.parentFirst = parentFirst;
    }

    public static Builder builder(String name, ClassLoader parent, boolean parentFirst) {
        return new Builder(name, parent, parentFirst);
    }

    private String sanitizeName(String name) {
        if (name.startsWith("/")) {
            return name.substring(1);
        }
        return name;
    }

    @Override
    public Enumeration<URL> getResources(String nm) throws IOException {
        String name = sanitizeName(nm);
        if (bannedResources.contains(name)) {
            return Collections.emptyEnumeration();
        }
        List<URL> resources = new ArrayList<>();

        if (parentFirst) {
            Enumeration<URL> res = parent.getResources(nm);
            while (res.hasMoreElements()) {
                resources.add(res.nextElement());
            }
        }
        //ClassPathElement[] providers = loadableResources.get(name);
        //if (providers != null) {
        //    for (ClassPathElement element : providers) {
        //        resources.add(element.getResource(nm).getUrl());
        //    }
        //}
        for (ClassPathElement i : elements) {
            ClassPathResource res = i.getResource(nm);
            if (res != null) {
                resources.add(res.getUrl());
            }
        }
        if (!parentFirst) {
            Enumeration<URL> res = parent.getResources(nm);
            while (res.hasMoreElements()) {
                resources.add(res.nextElement());
            }
        }
        return Collections.enumeration(resources);
    }

    @Override
    public URL getResource(String nm) {
        String name = sanitizeName(nm);
        if (bannedResources.contains(name)) {
            return null;
        }
        if (parentFirst) {
            URL res = parent.getResource(nm);
            if (res != null) {
                return res;
            }
        }
        //        ClassPathElement[] providers = loadableResources.get(name);
        //        if (providers != null) {
        //            return providers[0].getResource(nm).getUrl();
        //        }
        //TODO: because of dev mode we can't use the fast path her, we need to iterate
        for (ClassPathElement i : elements) {
            ClassPathResource res = i.getResource(name);
            if (res != null) {
                return res.getUrl();
            }
        }
        if (!parentFirst) {
            return parent.getResource(nm);
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String nm) {
        String name = sanitizeName(nm);
        if (bannedResources.contains(name)) {
            return null;
        }
        if (parentFirst) {
            InputStream res = parent.getResourceAsStream(name);
            if (res != null) {
                return res;
            }
        }
        //        ClassPathElement[] providers = loadableResources.get(name);
        //        if (providers != null) {
        //            return new ByteArrayInputStream(providers[0].getResource(nm).getData());
        //        }
        //TODO: because of dev mode we can't use the fast path her, we need to iterate
        for (ClassPathElement i : elements) {
            ClassPathResource res = i.getResource(name);
            if (res != null) {
                return new ByteArrayInputStream(res.getData());
            }
        }
        if (!parentFirst) {
            return parent.getResourceAsStream(nm);
        }
        return null;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            String resourceName = sanitizeName(name).replace(".", "/") + ".class";
            if (bannedResources.contains(resourceName)) {
                throw new ClassNotFoundException(name);
            }
            if (parentFirst) {
                try {
                    return parent.loadClass(name);
                } catch (ClassNotFoundException ignore) {
                    log.tracef("Class %s not found in parent first load from %s", name, parent);
                }
            }
            ClassPathElement[] resource = loadableResources.get(resourceName);
            if (resource != null) {
                ClassPathElement classPathElement = resource[0];
                byte[] data = classPathElement.getResource(resourceName).getData();
                List<BiFunction<String, ClassVisitor, ClassVisitor>> transformers = bytecodeTransformers.get(name);
                if (transformers != null) {
                    data = handleTransform(name, data, transformers);
                }
                definePackage(name);
                return defineClass(name, data, 0, data.length,
                        protectionDomains.computeIfAbsent(classPathElement, (ce) -> ce.getProtectionDomain(this)));
            }

            if (!parentFirst) {
                return parent.loadClass(name);
            }
            throw new ClassNotFoundException(name);
        }
    }

    private void definePackage(String name) {
        final String pkgName = getPackageNameFromClassName(name);
        if ((pkgName != null) && getPackage(pkgName) == null) {
            synchronized (getClassLoadingLock(pkgName)) {
                if (getPackage(pkgName) == null) {
                    // this could certainly be improved to use the actual manifest
                    definePackage(pkgName, null, null, null, null, null, null, null);
                }
            }
        }
    }

    private String getPackageNameFromClassName(String className) {
        final int index = className.lastIndexOf('.');
        if (index == -1) {
            // we return null here since in this case no package is defined
            // this is same behavior as Package.getPackage(clazz) exhibits
            // when the class is in the default package
            return null;
        }
        return className.substring(0, index);
    }

    private byte[] handleTransform(String name, byte[] bytes,
            List<BiFunction<String, ClassVisitor, ClassVisitor>> transformers) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter writer = new QuarkusClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = writer;
        for (BiFunction<String, ClassVisitor, ClassVisitor> i : transformers) {
            visitor = i.apply(name, visitor);
        }
        cr.accept(visitor, 0);
        return writer.toByteArray();
    }

    @SuppressWarnings("unused")
    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        return super.defineClass(name, b, off, len);
    }

    @Override
    public void close() throws IOException {
        for (ClassPathElement element : elements) {
            try (ClassPathElement ignored = element) {

            } catch (Exception e) {
                log.error("Failed to close " + element, e);
            }
        }
    }

    @Override
    public String toString() {
        return "QuarkusClassLoader:" + name;
    }

    public static class Builder {
        final String name;
        final ClassLoader parent;
        final List<ClassPathElement> elements = new ArrayList<>();
        final List<ClassPathElement> bannedElements = new ArrayList<>();
        Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = Collections.emptyMap();
        final boolean parentFirst;

        public Builder(String name, ClassLoader parent, boolean parentFirst) {
            this.name = name;
            this.parent = parent;
            this.parentFirst = parentFirst;
        }

        /**
         * Adds an element that can be used to load classes.
         *
         * Order is important, if there are multiple elements that provide the same
         * class then the first one passed to this method will be used.
         *
         * The provided element will be closed when the ClassLoader is closed.
         *
         * @param element The element to add
         * @return This builder
         */
        public Builder addElement(ClassPathElement element) {
            elements.add(element);
            return this;
        }

        /**
         * Adds an element that contains classes that should never be loaded by this loader.
         *
         * Note that elements passed to this method will not be automatically closed, as
         * references to this element are not retained after the class loader has been built.
         *
         * This is because banned elements are generally expected to be loaded by another ClassLoader,
         * so this prevents the need to create multiple ClassPathElements for the same resource.
         *
         * Banned elements have the highest priority, a banned element will never be loaded,
         * and resources will never appear to be present.
         * 
         * @param element The element to add
         * @return This builder
         */
        public Builder addBannedElement(ClassPathElement element) {
            bannedElements.add(element);
            return this;
        }

        /**
         * Sets any bytecode transformers that should be applied to this Class Loader
         * 
         * @param bytecodeTransformers
         */
        public void setBytecodeTransformers(
                Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers) {
            if (bytecodeTransformers == null) {
                this.bytecodeTransformers = Collections.emptyMap();
            } else {
                this.bytecodeTransformers = bytecodeTransformers;
            }
        }

        /**
         * Builds the class loader
         * 
         * @return The class loader
         */
        public QuarkusClassLoader build() {
            Map<String, List<ClassPathElement>> elementMap = new HashMap<>();
            for (ClassPathElement element : elements) {
                for (String i : element.getProvidedResources()) {
                    if (i.startsWith("/")) {
                        throw new RuntimeException(
                                "Resources cannot start with /, " + i + " is incorrect provided by " + element);
                    }
                    List<ClassPathElement> list = elementMap.get(i);
                    if (list == null) {
                        elementMap.put(i, list = new ArrayList<>());
                    }
                    list.add(element);
                }
            }
            Map<String, ClassPathElement[]> finalElements = new HashMap<>();
            for (Map.Entry<String, List<ClassPathElement>> i : elementMap.entrySet()) {
                finalElements.put(i.getKey(), i.getValue().toArray(new ClassPathElement[i.getValue().size()]));
            }
            Set<String> banned = new HashSet<>();
            for (ClassPathElement i : bannedElements) {
                banned.addAll(i.getProvidedResources());
            }
            return new QuarkusClassLoader(name, new ArrayList<>(elements), finalElements, bytecodeTransformers, banned, parent,
                    parentFirst);
        }
    }
}
