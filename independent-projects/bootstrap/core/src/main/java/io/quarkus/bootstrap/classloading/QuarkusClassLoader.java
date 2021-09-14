package io.quarkus.bootstrap.classloading;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.jboss.logging.Logger;

/**
 * The ClassLoader used for non production Quarkus applications (i.e. dev and test mode).
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
    private final ConcurrentMap<String, Package> definedPackages = new ConcurrentHashMap<>();
    private final ClassLoader parent;
    /**
     * If this is true it will attempt to load from the parent first
     */
    private final boolean parentFirst;
    private final boolean aggregateParentResources;
    private final List<ClassPathElement> bannedElements;
    private final List<ClassPathElement> parentFirstElements;
    private final List<ClassPathElement> lesserPriorityElements;
    private final List<ClassLoaderEventListener> classLoaderEventListeners;

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
    private volatile MemoryClassPathElement transformedClasses;
    private volatile ClassLoaderState state;
    private final List<Runnable> closeTasks = new ArrayList<>();

    static final ClassLoader PLATFORM_CLASS_LOADER;

    static {
        ClassLoader cl = null;
        try {
            cl = (ClassLoader) ClassLoader.class.getDeclaredMethod("getPlatformClassLoader").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {

        }
        PLATFORM_CLASS_LOADER = cl;
    }

    private boolean closed;
    private volatile boolean driverLoaded;

    private QuarkusClassLoader(Builder builder) {
        super(builder.parent);
        this.name = builder.name;
        this.elements = builder.elements;
        this.bannedElements = builder.bannedElements;
        this.parentFirstElements = builder.parentFirstElements;
        this.lesserPriorityElements = builder.lesserPriorityElements;
        this.parent = builder.parent;
        this.parentFirst = builder.parentFirst;
        this.resettableElement = builder.resettableElement;
        this.transformedClasses = new MemoryClassPathElement(builder.transformedClasses);
        this.aggregateParentResources = builder.aggregateParentResources;
        this.classLoaderEventListeners = builder.classLoaderEventListeners.isEmpty() ? Collections.emptyList()
                : builder.classLoaderEventListeners;
    }

    public static Builder builder(String name, ClassLoader parent, boolean parentFirst) {
        return new Builder(name, parent, parentFirst);
    }

    private String sanitizeName(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    /**
     * Returns true if the supplied class is a class that would be loaded parent-first
     */
    public boolean isParentFirst(String name) {
        if (name.startsWith(JAVA)) {
            return true;
        }

        //even if the thread is interrupted we still want to be able to load classes
        //if the interrupt bit is set then we clear it and restore it at the end
        boolean interrupted = Thread.interrupted();
        try {
            ClassLoaderState state = getState();
            synchronized (getClassLoadingLock(name)) {
                String resourceName = sanitizeName(name).replace('.', '/') + ".class";
                return parentFirst(resourceName, state);
            }

        } finally {
            if (interrupted) {
                //restore interrupt state
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean parentFirst(String name, ClassLoaderState state) {
        return parentFirst || state.parentFirstResources.contains(name);
    }

    public void reset(Map<String, byte[]> generatedResources, Map<String, byte[]> transformedClasses) {
        if (resettableElement == null) {
            throw new IllegalStateException("Classloader is not resettable");
        }
        synchronized (this) {
            this.transformedClasses = new MemoryClassPathElement(transformedClasses);
            resettableElement.reset(generatedResources);
            state = null;
        }
    }

    @Override
    public Enumeration<URL> getResources(String unsanitisedName) throws IOException {
        for (ClassLoaderEventListener l : classLoaderEventListeners) {
            l.enumeratingResourceURLs(unsanitisedName, this.name);
        }
        boolean endsWithTrailingSlash = unsanitisedName.endsWith("/");
        ClassLoaderState state = getState();
        String name = sanitizeName(unsanitisedName);
        //for resources banned means that we don't delegate to the parent, as there can be multiple resources
        //for single resources we still respect this
        boolean banned = state.bannedResources.contains(name);
        Set<URL> resources = new LinkedHashSet<>();

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
        //TODO: in theory resources could have been added in dev mode
        //but I don't thing this really matters for this code path
        ClassPathElement[] providers = state.loadableResources.get(name);
        if (providers != null) {
            for (ClassPathElement element : providers) {
                ClassPathResource res = element.getResource(name);
                //if the requested name ends with a trailing / we make sure
                //that the resource is a directory, and return a URL that ends with a /
                //this matches the behaviour of URLClassLoader
                if (endsWithTrailingSlash) {
                    if (res.isDirectory()) {
                        try {
                            resources.add(new URL(res.getUrl().toString() + "/"));
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    resources.add(res.getUrl());
                }
            }
        } else if (name.isEmpty()) {
            for (ClassPathElement i : elements) {
                ClassPathResource res = i.getResource("");
                if (res != null) {
                    resources.add(res.getUrl());
                }
            }
        }
        if (!banned) {
            if (resources.isEmpty() || aggregateParentResources) {
                Enumeration<URL> res = parent.getResources(unsanitisedName);
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
                            if (transformedClasses.getResource(i) != null) {
                                elementMap.put(i, Collections.singletonList(transformedClasses));
                            } else {
                                List<ClassPathElement> list = elementMap.get(i);
                                if (list == null) {
                                    elementMap.put(i, list = new ArrayList<>(2)); //default initial capacity of 10 is way too large
                                }
                                list.add(element);
                            }
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
    public URL getResource(String unsanitisedName) {
        for (ClassLoaderEventListener l : classLoaderEventListeners) {
            l.gettingURLFromResource(unsanitisedName, this.name);
        }
        boolean endsWithTrailingSlash = unsanitisedName.endsWith("/");
        String name = sanitizeName(unsanitisedName);
        ClassLoaderState state = getState();
        if (state.bannedResources.contains(name)) {
            return null;
        }
        //TODO: because of dev mode we iterate, to see if any resources were added
        //not for .class files though, adding them causes a restart
        //this is very important for bytebuddy performance
        if (name.endsWith(".class") && !endsWithTrailingSlash) {
            ClassPathElement[] providers = state.loadableResources.get(name);
            if (providers != null) {
                return providers[0].getResource(name).getUrl();
            }
        } else {
            for (ClassPathElement i : elements) {
                ClassPathResource res = i.getResource(name);
                if (res != null) {
                    //if the requested name ends with a trailing / we make sure
                    //that the resource is a directory, and return a URL that ends with a /
                    //this matches the behaviour of URLClassLoader
                    if (endsWithTrailingSlash) {
                        if (res.isDirectory()) {
                            try {
                                return new URL(res.getUrl().toString() + "/");
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        return res.getUrl();
                    }
                }
            }
        }
        return parent.getResource(unsanitisedName);
    }

    @Override
    public InputStream getResourceAsStream(String unsanitisedName) {
        for (ClassLoaderEventListener l : classLoaderEventListeners) {
            l.openResourceStream(unsanitisedName, this.name);
        }
        String name = sanitizeName(unsanitisedName);
        ClassLoaderState state = getState();
        if (state.bannedResources.contains(name)) {
            return null;
        }
        //dev mode may have added some files, so we iterate to check, but not for classes
        if (name.endsWith(".class")) {
            ClassPathElement[] providers = state.loadableResources.get(name);
            if (providers != null) {
                return new ByteArrayInputStream(providers[0].getResource(name).getData());
            }
        } else {
            for (ClassPathElement i : elements) {
                ClassPathResource res = i.getResource(name);
                if (res != null) {
                    if (res.isDirectory()) {
                        try {
                            return res.getUrl().openStream();
                        } catch (IOException e) {
                            log.debug("Ignoring exception that occurred while opening a stream for resource " + unsanitisedName,
                                    e);
                            // behave like how java.lang.ClassLoader#getResourceAsStream() behaves
                            // and don't propagate the exception
                            continue;
                        }
                    }
                    return new ByteArrayInputStream(res.getData());
                }
            }
        }
        return parent.getResourceAsStream(unsanitisedName);
    }

    /**
     * This method is needed to make packages work correctly on JDK9+, as it will be called
     * to load the package-info class.
     *
     * @param moduleName
     * @param name
     * @return
     */
    @Override
    protected Class<?> findClass(String moduleName, String name) {
        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoaderEventListener l : classLoaderEventListeners) {
            l.loadClass(name, this.name);
        }
        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (ClassLoaderEventListener l : classLoaderEventListeners) {
            l.loadClass(name, this.name);
        }
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
                String resourceName = sanitizeName(name).replace('.', '/') + ".class";
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
                        definePackage(name, classPathElement);
                        Class<?> cl = defineClass(name, data, 0, data.length,
                                protectionDomains.computeIfAbsent(classPathElement, (ce) -> ce.getProtectionDomain(this)));
                        if (Driver.class.isAssignableFrom(cl)) {
                            driverLoaded = true;
                        }
                        return cl;
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
        //we can't use getPackage here
        //if can return a package from the parent
        if ((pkgName != null) && definedPackages.get(pkgName) == null) {
            synchronized (getClassLoadingLock(pkgName)) {
                if (definedPackages.get(pkgName) == null) {
                    Manifest mf = classPathElement.getManifest();
                    if (mf != null) {
                        Attributes ma = mf.getMainAttributes();
                        definedPackages.put(pkgName, definePackage(pkgName, ma.getValue(Attributes.Name.SPECIFICATION_TITLE),
                                ma.getValue(Attributes.Name.SPECIFICATION_VERSION),
                                ma.getValue(Attributes.Name.SPECIFICATION_VENDOR),
                                ma.getValue(Attributes.Name.IMPLEMENTATION_TITLE),
                                ma.getValue(Attributes.Name.IMPLEMENTATION_VERSION),
                                ma.getValue(Attributes.Name.IMPLEMENTATION_VENDOR), null));
                        return;
                    }

                    // this could certainly be improved to use the actual manifest
                    definedPackages.put(pkgName, definePackage(pkgName, null, null, null, null, null, null, null));
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

    public List<ClassPathElement> getElementsWithResource(String name) {
        return getElementsWithResource(name, false);
    }

    public List<ClassPathElement> getElementsWithResource(String name, boolean localOnly) {
        List<ClassPathElement> ret = new ArrayList<>();
        if (parent instanceof QuarkusClassLoader && !localOnly) {
            ret.addAll(((QuarkusClassLoader) parent).getElementsWithResource(name));
        }
        ClassPathElement[] classPathElements = getState().loadableResources.get(name);
        if (classPathElements == null) {
            return ret;
        }
        ret.addAll(Arrays.asList(classPathElements));
        return ret;
    }

    public List<String> getLocalClassNames() {
        List<String> ret = new ArrayList<>();
        for (String name : getState().loadableResources.keySet()) {
            if (name.endsWith(".class")) {
                ret.add(name.substring(0, name.length() - 6).replace('/', '.'));
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        return super.defineClass(name, b, off, len);
    }

    public void addCloseTask(Runnable task) {
        closeTasks.add(task);
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        for (Runnable i : closeTasks) {
            try {
                i.run();
            } catch (Throwable t) {
                log.error("Failed to run close task", t);
            }
        }
        if (driverLoaded) {
            //DriverManager only lets you remove drivers with the same CL as the caller
            //so we need do define the cleaner in this class loader
            try (InputStream is = getClass().getResourceAsStream("DriverRemover.class")) {
                byte[] data = JarClassPathElement.readStreamContents(is);
                Runnable r = (Runnable) defineClass(DriverRemover.class.getName(), data, 0, data.length)
                        .getConstructor(ClassLoader.class).newInstance(this);
                r.run();
            } catch (Exception e) {
                log.debug("Failed to clean up DB drivers");
            }
        }
        for (ClassPathElement element : elements) {
            //note that this is a 'soft' close
            //all resources are closed, however the CL can still be used
            //but after close no resources will be held past the scope of an operation
            try (ClassPathElement ignored = element) {
                //the close() operation is implied by the try-with syntax
            } catch (Exception e) {
                log.error("Failed to close " + element, e);
            }
        }
        for (ClassPathElement element : bannedElements) {
            //note that this is a 'soft' close
            //all resources are closed, however the CL can still be used
            //but after close no resources will be held past the scope of an operation
            try (ClassPathElement ignored = element) {
                //the close() operation is implied by the try-with syntax
            } catch (Exception e) {
                log.error("Failed to close " + element, e);
            }
        }
        ResourceBundle.clearCache(this);

    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "QuarkusClassLoader:" + name + "@" + Integer.toHexString(hashCode());
    }

    public static class Builder {
        final String name;
        final ClassLoader parent;
        final List<ClassPathElement> elements = new ArrayList<>();
        final List<ClassPathElement> bannedElements = new ArrayList<>();
        final List<ClassPathElement> parentFirstElements = new ArrayList<>();
        final List<ClassPathElement> lesserPriorityElements = new ArrayList<>();
        final boolean parentFirst;
        MemoryClassPathElement resettableElement;
        private Map<String, byte[]> transformedClasses = Collections.emptyMap();
        boolean aggregateParentResources;
        private final ArrayList<ClassLoaderEventListener> classLoaderEventListeners = new ArrayList<>(5);

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

        public Builder setTransformedClasses(Map<String, byte[]> transformedClasses) {
            this.transformedClasses = transformedClasses;
            return this;
        }

        public Builder addClassLoaderEventListeners(List<ClassLoaderEventListener> classLoadListeners) {
            this.classLoaderEventListeners.addAll(classLoadListeners);
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
            this.classLoaderEventListeners.trimToSize();
            return new QuarkusClassLoader(this);
        }

    }

    public ClassLoader parent() {
        return parent;
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

    @Override
    public String getName() {
        return name;
    }

}
