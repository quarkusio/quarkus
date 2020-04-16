package io.quarkus.bootstrap.classloading;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * The ClassLoader used for non production Quarkus applications (i.e. dev and test mode). Production
 * applications use a flat classpath so just use the system class loader.
 *
 *
 */
public class QuarkusClassLoader extends ClassLoader implements Closeable {

    private static final Logger log = Logger.getLogger(QuarkusClassLoader.class);
    protected static final String META_INF_SERVICES = "META-INF/services/";
    protected static final String JAVA = "java.";

    static {
        registerAsParallelCapable();
    }

    private final String name;
    private final List<ClassPathElement> elements;
    private final ConcurrentMap<ClassPathElement, ProtectionDomain> protectionDomains = new ConcurrentHashMap<>();
    private final ClassLoader parent;
    /**
     * If this is true it will attempt to load from the parent first
     */
    private final boolean parentFirst;
    private final boolean aggregateParentResources;
    private final List<ClassPathElement> bannedElements;
    private final List<ClassPathElement> parentFirstElements;
    private final List<ClassPathElement> lesserPriorityElements;

    /**
     * The element that holds resettable in-memory classses.
     *
     * A reset occurs when new transformers and in-memory classes are added to a ClassLoader. It happens after each
     * start in dev mode, however in general the reset resources will be the same. There are some cases where this is
     * not the case though:
     *
     * - Dev mode failed start will not end up with transformers or generated classes being registered. The reset
     * in this case will add them.
     * - Platform CDI beans that are considered to be removed and have the removed status changed will result in
     * additional classes being added to the class loader.
     *
     */
    private volatile MemoryClassPathElement resettableElement;

    private volatile Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers;
    private volatile ClassLoader transformerClassLoader;
    private volatile ClassLoaderState state;

    private QuarkusClassLoader(Builder builder) {
        //we need the parent to be null
        //as MP has super broken class loading where it attempts to resolve stuff from the parent
        //will hopefully be fixed in 1.4
        //e.g. https://github.com/eclipse/microprofile-config/issues/390
        //e.g. https://github.com/eclipse/microprofile-reactive-streams-operators/pull/130
        super(null);
        this.name = builder.name;
        this.elements = builder.elements;
        this.bytecodeTransformers = builder.bytecodeTransformers;
        this.bannedElements = builder.bannedElements;
        this.parentFirstElements = builder.parentFirstElements;
        this.lesserPriorityElements = builder.lesserPriorityElements;
        this.parent = builder.parent;
        this.parentFirst = builder.parentFirst;
        this.resettableElement = builder.resettableElement;
        this.transformerClassLoader = builder.transformerClassLoader;
        this.aggregateParentResources = builder.aggregateParentResources;
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

    private boolean parentFirst(String name, ClassLoaderState state) {
        return parentFirst || state.parentFirstResources.contains(name);
    }

    public void reset(Map<String, byte[]> resources,
            Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers,
            ClassLoader transformerClassLoader) {
        if (resettableElement == null) {
            throw new IllegalStateException("Classloader is no resettable");
        }
        this.transformerClassLoader = transformerClassLoader;
        synchronized (this) {
            resettableElement.reset(resources);
            this.bytecodeTransformers = bytecodeTransformers;
            state = null;
        }
    }

    @Override
    public Enumeration<URL> getResources(String nm) throws IOException {
        ClassLoaderState state = getState();
        String name = sanitizeName(nm);
        //for resources banned means that we don't delegate to the parent, as there can be multiple resources
        //for single resources we still respect this
        boolean banned = state.bannedResources.contains(name);
        Set<URL> resources = new LinkedHashSet<>();
        //ClassPathElement[] providers = loadableResources.get(name);
        //if (providers != null) {
        //    for (ClassPathElement element : providers) {
        //        resources.add(element.getResource(nm).getUrl());
        //    }
        //}

        //this is a big of a hack, but is necessary to prevent service leakage
        //in some situations (looking at you gradle) the parent can contain the same
        //classes as the application. The parent aggregation stops this being a problem
        //in most cases, however if there are no actual implementations of the service
        //in the application then this can still cause problems
        //this approach makes sure that we don't load services that would result
        //in a ServiceConfigurationError
        //see https://github.com/quarkusio/quarkus/issues/7996
        if (name.startsWith(META_INF_SERVICES)) {
            try {
                Class<?> c = loadClass(name.substring(META_INF_SERVICES.length()));
                if (c.getClassLoader() == this) {
                    //if the service class is defined by this class loader then any resources that could be loaded
                    //by the parent would have a different copy of the service class
                    banned = true;
                }
            } catch (ClassNotFoundException ignored) {
                //ignore
            }
        }
        for (ClassPathElement i : elements) {
            ClassPathResource res = i.getResource(nm);
            if (res != null) {
                resources.add(res.getUrl());
            }
        }
        if (!banned) {
            if (resources.isEmpty() || aggregateParentResources) {
                Enumeration<URL> res = parent.getResources(nm);
                while (res.hasMoreElements()) {
                    resources.add(res.nextElement());
                }
            }
        }
        return Collections.enumeration(resources);
    }

    private ClassLoaderState getState() {
        ClassLoaderState state = this.state;
        if (state == null) {
            synchronized (this) {
                state = this.state;
                if (state == null) {
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
                        List<ClassPathElement> entryClassPathElements = i.getValue();
                        if (!lesserPriorityElements.isEmpty() && (entryClassPathElements.size() > 1)) {
                            List<ClassPathElement> entryNormalPriorityElements = new ArrayList<>(entryClassPathElements.size());
                            List<ClassPathElement> entryLesserPriorityElements = new ArrayList<>(entryClassPathElements.size());
                            for (ClassPathElement classPathElement : entryClassPathElements) {
                                if (lesserPriorityElements.contains(classPathElement)) {
                                    entryLesserPriorityElements.add(classPathElement);
                                } else {
                                    entryNormalPriorityElements.add(classPathElement);
                                }
                            }
                            // ensure the lesser priority elements are added later
                            entryClassPathElements = new ArrayList<>(entryClassPathElements.size());
                            entryClassPathElements.addAll(entryNormalPriorityElements);
                            entryClassPathElements.addAll(entryLesserPriorityElements);
                        }
                        finalElements.put(i.getKey(),
                                entryClassPathElements.toArray(new ClassPathElement[entryClassPathElements.size()]));
                    }
                    Set<String> banned = new HashSet<>();
                    for (ClassPathElement i : bannedElements) {
                        banned.addAll(i.getProvidedResources());
                    }
                    Set<String> parentFirstResources = new HashSet<>();
                    for (ClassPathElement i : parentFirstElements) {
                        parentFirstResources.addAll(i.getProvidedResources());
                    }
                    return this.state = new ClassLoaderState(finalElements, banned, parentFirstResources);
                }
            }
        }
        return state;
    }

    @Override
    public URL getResource(String nm) {
        String name = sanitizeName(nm);
        ClassLoaderState state = getState();
        if (state.bannedResources.contains(name)) {
            return null;
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
        return parent.getResource(nm);
    }

    @Override
    public InputStream getResourceAsStream(String nm) {
        String name = sanitizeName(nm);
        ClassLoaderState state = getState();
        if (state.bannedResources.contains(name)) {
            return null;
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
        return parent.getResourceAsStream(nm);
    }

    /**
     * This method is needed to make packages work correctly on JDK9+, as it will be called
     * to load the package-info class.
     *
     * @param moduleName
     * @param name
     * @return
     */
    //@Override
    protected Class<?> findClass(String moduleName, String name) {
        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith(JAVA)) {
            return parent.loadClass(name);
        }
        //even if the thread is interrupted we still want to be able to load classes
        //if the interrupt bit is set then we clear it and restore it at the end
        boolean interrupted = Thread.interrupted();
        try {
            ClassLoaderState state = getState();
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c != null) {
                    return c;
                }
                String resourceName = sanitizeName(name).replace(".", "/") + ".class";
                boolean parentFirst = parentFirst(resourceName, state);
                if (state.bannedResources.contains(resourceName)) {
                    throw new ClassNotFoundException(name);
                }
                if (parentFirst) {
                    try {
                        return parent.loadClass(name);
                    } catch (ClassNotFoundException ignore) {
                        log.tracef("Class %s not found in parent first load from %s", name, parent);
                    }
                }
                ClassPathElement[] resource = state.loadableResources.get(resourceName);
                if (resource != null) {
                    ClassPathElement classPathElement = resource[0];
                    ClassPathResource classPathElementResource = classPathElement.getResource(resourceName);
                    if (classPathElementResource != null) { //can happen if the class loader was closed
                        byte[] data = classPathElementResource.getData();
                        List<BiFunction<String, ClassVisitor, ClassVisitor>> transformers = bytecodeTransformers.get(name);
                        if (transformers != null) {
                            data = handleTransform(name, data, transformers);
                        }
                        definePackage(name, classPathElement);
                        return defineClass(name, data, 0, data.length,
                                protectionDomains.computeIfAbsent(classPathElement, (ce) -> ce.getProtectionDomain(this)));
                    }
                }

                if (!parentFirst) {
                    return parent.loadClass(name);
                }
                throw new ClassNotFoundException(name);
            }

        } finally {
            if (interrupted) {
                //restore interrupt state
                Thread.currentThread().interrupt();
            }
        }
    }

    private void definePackage(String name, ClassPathElement classPathElement) {
        final String pkgName = getPackageNameFromClassName(name);
        if ((pkgName != null) && getPackage(pkgName) == null) {
            synchronized (getClassLoadingLock(pkgName)) {
                if (getPackage(pkgName) == null) {
                    Manifest mf = classPathElement.getManifest();
                    if (mf != null) {
                        Attributes ma = mf.getMainAttributes();
                        definePackage(pkgName, ma.getValue(Attributes.Name.SPECIFICATION_TITLE),
                                ma.getValue(Attributes.Name.SPECIFICATION_VERSION),
                                ma.getValue(Attributes.Name.SPECIFICATION_VENDOR),
                                ma.getValue(Attributes.Name.IMPLEMENTATION_TITLE),
                                ma.getValue(Attributes.Name.IMPLEMENTATION_VERSION),
                                ma.getValue(Attributes.Name.IMPLEMENTATION_VENDOR), null);
                        return;
                    }

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
        ClassWriter writer = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {

            @Override
            protected ClassLoader getClassLoader() {
                return transformerClassLoader;
            }
        };
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
    public void close() {
        for (ClassPathElement element : elements) {
            //note that this is a 'soft' close
            //all resources are closed, however the CL can still be used
            //but after close no resources will be held past the scope of an operation
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
        final List<ClassPathElement> parentFirstElements = new ArrayList<>();
        final List<ClassPathElement> lesserPriorityElements = new ArrayList<>();
        Map<String, List<BiFunction<String, ClassVisitor, ClassVisitor>>> bytecodeTransformers = Collections.emptyMap();
        final boolean parentFirst;
        MemoryClassPathElement resettableElement;
        private volatile ClassLoader transformerClassLoader;
        boolean aggregateParentResources;

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
            log.debugf("Adding elements %s to QuarkusClassLoader %s", element, name);
            elements.add(element);
            return this;
        }

        /**
         * Adds a resettable MemoryClassPathElement to the class loader.
         *
         * This element is mutable, and its contents will be modified if the class loader
         * is reset.
         *
         * If this is not explicitly added to the elements list then it will be automatically
         * added as the highest priority element.
         *
         * @param resettableElement The element
         * @return This builder
         */
        public Builder setResettableElement(MemoryClassPathElement resettableElement) {
            this.resettableElement = resettableElement;
            return this;
        }

        /**
         * Adds an element that contains classes that will always be loaded in a parent first manner.
         *
         * Note that this does not mean that the parent will always have this class, it is possible that
         * in some cases the class will end up being loaded by this loader, however an attempt will always
         * be made to load these from the parent CL first
         *
         * Note that elements passed to this method will not be automatically closed, as
         * references to this element are not retained after the class loader has been built.
         *
         * @param element The element to add
         * @return This builder
         */
        public Builder addParentFirstElement(ClassPathElement element) {
            log.debugf("Adding parent first element %s to QuarkusClassLoader %s", element, name);
            parentFirstElements.add(element);
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
         * Adds an element which will only be used to load a class or resource if no normal
         * element containing that class or resource exists.
         * This is used in order control the order of elements when multiple contain the same classes
         *
         * @param element The element to add
         * @return This builder
         */
        public Builder addLesserPriorityElement(ClassPathElement element) {
            lesserPriorityElements.add(element);
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
         * If this is true then a getResources call will always include the parent resources.
         *
         * If this is false then getResources will not return parent resources if local resources were found.
         *
         * This only takes effect if parentFirst is false.
         */
        public Builder setAggregateParentResources(boolean aggregateParentResources) {
            this.aggregateParentResources = aggregateParentResources;
            return this;
        }

        public Builder setTransformerClassLoader(ClassLoader transformerClassLoader) {
            this.transformerClassLoader = transformerClassLoader;
            return this;
        }

        /**
         * Builds the class loader
         *
         * @return The class loader
         */
        public QuarkusClassLoader build() {
            if (resettableElement != null) {
                if (!elements.contains(resettableElement)) {
                    elements.add(0, resettableElement);
                }
            }
            return new QuarkusClassLoader(this);
        }
    }

    static final class ClassLoaderState {

        final Map<String, ClassPathElement[]> loadableResources;
        final Set<String> bannedResources;
        final Set<String> parentFirstResources;

        ClassLoaderState(Map<String, ClassPathElement[]> loadableResources, Set<String> bannedResources,
                Set<String> parentFirstResources) {
            this.loadableResources = loadableResources;
            this.bannedResources = bannedResources;
            this.parentFirstResources = parentFirstResources;
        }
    }
}
