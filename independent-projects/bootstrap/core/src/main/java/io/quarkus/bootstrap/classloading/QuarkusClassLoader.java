package io.quarkus.bootstrap.classloading;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;
import static io.quarkus.commons.classloading.ClassLoaderHelper.isInJdkPackage;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import com.google.common.annotations.VisibleForTesting;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.paths.PathVisit;

/**
 * The ClassLoader used for non production Quarkus applications (i.e. dev and test mode).
 */
public class QuarkusClassLoader extends ClassLoader implements Closeable {
    private static final Logger log = Logger.getLogger(QuarkusClassLoader.class);
    private static final Logger lifecycleLog = Logger.getLogger(QuarkusClassLoader.class.getName() + ".lifecycle");
    private static final boolean LOG_ACCESS_TO_CLOSED_CLASS_LOADERS = Boolean
            .getBoolean("quarkus-log-access-to-closed-class-loaders");

    private static final byte STATUS_OPEN = 1;
    private static final byte STATUS_CLOSING = 0;
    private static final byte STATUS_CLOSED = -1;

    protected static final String META_INF_SERVICES = "META-INF/services/";

    private final CuratedApplication curatedApplication;
    private StartupAction startupAction;

    static {
        registerAsParallelCapable();
    }

    private static RuntimeException nonQuarkusClassLoaderError() {
        return new IllegalStateException("The current classloader is not an instance of "
                + QuarkusClassLoader.class.getName() + " but "
                + Thread.currentThread().getContextClassLoader().getClass().getName());
    }

    /**
     * Visits every found runtime resource with a given name. If a resource is not found, the visitor will
     * simply not be called.
     * <p>
     * IMPORTANT: this method works only when the current class loader is an instance of {@link QuarkusClassLoader},
     * otherwise it throws an error with the corresponding message.
     *
     * @param resourceName runtime resource name to visit
     * @param visitor runtime resource visitor
     */
    public static void visitRuntimeResources(String resourceName, Consumer<PathVisit> visitor) {
        if (Thread.currentThread().getContextClassLoader() instanceof QuarkusClassLoader classLoader) {
            for (var element : classLoader.getElementsWithResource(resourceName)) {
                if (element.isRuntime()) {
                    element.apply(tree -> {
                        tree.accept(resourceName, visitor);
                        return null;
                    });
                }
            }
        } else {
            throw nonQuarkusClassLoaderError();
        }
    }

    public static List<ClassPathElement> getElements(String resourceName, boolean onlyFromCurrentClassLoader) {
        if (Thread.currentThread().getContextClassLoader() instanceof QuarkusClassLoader classLoader) {
            return classLoader.getElementsWithResource(resourceName, onlyFromCurrentClassLoader);
        }
        throw nonQuarkusClassLoaderError();
    }

    /**
     * Indicates if a given class is present at runtime.
     *
     * @param className the name of the class.
     */
    public static boolean isClassPresentAtRuntime(String className) {
        String resourceName = fromClassNameToResourceName(className);
        return isResourcePresentAtRuntime(resourceName);
    }

    /**
     * Indicates if a given class is considered an application class.
     */
    public static boolean isApplicationClass(String className) {
        if (Thread.currentThread().getContextClassLoader() instanceof QuarkusClassLoader classLoader) {
            String resourceName = fromClassNameToResourceName(className);
            ClassPathResourceIndex classPathResourceIndex = classLoader.getClassPathResourceIndex();

            return classPathResourceIndex.getFirstClassPathElement(resourceName) != null;
        }
        throw nonQuarkusClassLoaderError();
    }

    /**
     * Indicates if a given resource is present at runtime.
     * Can also be used to check if a class is present as a class is just a regular resource.
     *
     * @param resourcePath the path of the resource, for instance {@code path/to/my-resources.properties} for a properties file
     *        or {@code my/package/MyClass.class} for a class.
     */
    public static boolean isResourcePresentAtRuntime(String resourcePath) {
        List<ClassPathElement> classPathElements = QuarkusClassLoader.getElements(resourcePath, false);
        for (int i = 0; i < classPathElements.size(); i++) {
            if (classPathElements.get(i).isRuntime()) {
                return true;
            }
        }

        return false;
    }

    private final String name;
    // the ClassPathElements to consider are normalPriorityElements + lesserPriorityElements
    private final List<ClassPathElement> normalPriorityElements;
    private final List<ClassPathElement> lesserPriorityElements;
    private final List<ClassPathElement> bannedElements;
    private final List<ClassPathElement> parentFirstElements;
    private final ConcurrentMap<ClassPathElement, ProtectionDomain> protectionDomains = new ConcurrentHashMap<>();
    private final ClassLoader parent;
    /**
     * If this is true it will attempt to load from the parent first
     */
    private final boolean parentFirst;
    private final boolean aggregateParentResources;
    private final List<ClassLoaderEventListener> classLoaderEventListeners;

    /**
     * The element that holds resettable in-memory classes.
     * <p>
     * A reset occurs when new transformers and in-memory classes are added to a ClassLoader. It happens after each
     * start in dev mode, however in general the reset resources will be the same. There are some cases where this is
     * not the case though:
     * <p>
     * - Dev mode failed start will not end up with transformers or generated classes being registered. The reset
     * in this case will add them.
     * - Platform CDI beans that are considered to be removed and have the removed status changed will result in
     * additional classes being added to the class loader.
     */
    private volatile MemoryClassPathElement resettableElement;
    private volatile MemoryClassPathElement transformedClasses;
    private volatile ClassPathResourceIndex classPathResourceIndex;
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

    private volatile byte status;
    private volatile boolean driverLoaded;

    private QuarkusClassLoader(Builder builder) {
        // Not passing the name to the parent constructor on purpose:
        // stacktraces become very ugly if we do that.
        super(builder.parent);
        this.name = builder.name;
        this.status = STATUS_OPEN;
        this.normalPriorityElements = builder.normalPriorityElements;
        this.bannedElements = builder.bannedElements;
        this.parentFirstElements = builder.parentFirstElements;
        this.lesserPriorityElements = builder.lesserPriorityElements;
        this.parent = builder.parent;
        this.parentFirst = builder.parentFirst;
        this.resettableElement = builder.resettableElement;
        this.transformedClasses = new MemoryClassPathElement(builder.transformedClasses, true);
        this.aggregateParentResources = builder.aggregateParentResources;
        this.classLoaderEventListeners = builder.classLoaderEventListeners.isEmpty() ? Collections.emptyList()
                : builder.classLoaderEventListeners;
        this.curatedApplication = builder.curatedApplication;
        setDefaultAssertionStatus(builder.assertionsEnabled);

        if (lifecycleLog.isDebugEnabled()) {
            lifecycleLog.debugf(new RuntimeException("Created to log a stacktrace"), "Creating class loader %s", this);
        }
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

    private boolean parentFirst(String name, ClassPathResourceIndex classPathResourceIndex) {
        return parentFirst || name.startsWith("io/quarkus/devservices/crossclassloader")
                || classPathResourceIndex.isParentFirst(name);
    }

    public void reset(Map<String, byte[]> generatedResources, Map<String, byte[]> transformedClasses) {
        ensureOpen();

        if (resettableElement == null) {
            throw new IllegalStateException("Classloader is not resettable");
        }
        synchronized (this) {
            this.transformedClasses = new MemoryClassPathElement(transformedClasses, true);
            resettableElement.reset(generatedResources);
            classPathResourceIndex = null;
        }
    }

    @Override
    public Enumeration<URL> getResources(String unsanitisedName) throws IOException {
        ensureOpen(unsanitisedName);

        return getResources(unsanitisedName, false);
    }

    public Enumeration<URL> getResources(String unsanitisedName, boolean parentAlreadyFoundResources) throws IOException {
        ensureOpen(unsanitisedName);

        for (int i = 0; i < classLoaderEventListeners.size(); i++) {
            classLoaderEventListeners.get(i).enumeratingResourceURLs(unsanitisedName, this.name);
        }
        ClassPathResourceIndex classPathResourceIndex = getClassPathResourceIndex();
        String name = sanitizeName(unsanitisedName);
        //for resources banned means that we don't delegate to the parent, as there can be multiple resources
        //for single resources we still respect this
        boolean banned = classPathResourceIndex.isBanned(name);

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
        //but I don't think this really matters for this code path
        Set<URL> resources = new LinkedHashSet<>();
        List<ClassPathElement> classPathElements = classPathResourceIndex.getClassPathElements(name);
        if (!classPathElements.isEmpty()) {
            boolean endsWithTrailingSlash = unsanitisedName.endsWith("/");
            for (int i = 0; i < classPathElements.size(); i++) {
                List<ClassPathResource> resList = classPathElements.get(i).getResources(name);
                //if the requested name ends with a trailing / we make sure
                //that the resource is a directory, and return a URL that ends with a /
                //this matches the behaviour of URLClassLoader
                for (int j = 0; j < resList.size(); j++) {
                    var res = resList.get(j);
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
            }
        }
        if (!banned) {
            if ((resources.isEmpty() && !parentAlreadyFoundResources) || aggregateParentResources) {
                Enumeration<URL> res;
                if (parent instanceof QuarkusClassLoader) {
                    res = ((QuarkusClassLoader) parent).getResources(unsanitisedName, !resources.isEmpty());
                } else {
                    res = parent.getResources(unsanitisedName);
                }
                while (res.hasMoreElements()) {
                    resources.add(res.nextElement());
                }
            }
        }
        return Collections.enumeration(resources);
    }

    private ClassPathResourceIndex getClassPathResourceIndex() {
        ClassPathResourceIndex classPathResourceIndex = this.classPathResourceIndex;
        if (classPathResourceIndex == null) {
            synchronized (this) {
                classPathResourceIndex = this.classPathResourceIndex;
                if (classPathResourceIndex == null) {
                    ClassPathResourceIndex.Builder classPathResourceIndexBuilder = ClassPathResourceIndex.builder();

                    classPathResourceIndexBuilder.scanClassPathElement(transformedClasses,
                            classPathResourceIndexBuilder::addTransformedClassCandidate);

                    for (ClassPathElement element : normalPriorityElements) {
                        classPathResourceIndexBuilder.scanClassPathElement(element,
                                classPathResourceIndexBuilder::addResourceMapping);
                    }

                    for (ClassPathElement lesserPriorityElement : lesserPriorityElements) {
                        classPathResourceIndexBuilder.scanClassPathElement(lesserPriorityElement,
                                classPathResourceIndexBuilder::addResourceMapping);
                    }

                    for (ClassPathElement bannedElement : bannedElements) {
                        classPathResourceIndexBuilder.scanClassPathElement(bannedElement,
                                (classPathElement, resource) -> classPathResourceIndexBuilder.addBannedResource(
                                        resource));
                    }

                    for (ClassPathElement parentFirstElement : parentFirstElements) {
                        classPathResourceIndexBuilder.scanClassPathElement(parentFirstElement,
                                (classPathElement, resource) -> classPathResourceIndexBuilder.addParentFirstResource(
                                        resource));
                    }

                    return this.classPathResourceIndex = classPathResourceIndexBuilder.build();
                }
            }
        }
        return classPathResourceIndex;
    }

    @Override
    public URL getResource(String unsanitisedName) {
        ensureOpen(unsanitisedName);

        for (ClassLoaderEventListener l : classLoaderEventListeners) {
            l.gettingURLFromResource(unsanitisedName, this.name);
        }
        String name = sanitizeName(unsanitisedName);
        ClassPathResourceIndex classPathResourceIndex = getClassPathResourceIndex();
        if (classPathResourceIndex.isBanned(name)) {
            return null;
        }
        //TODO: because of dev mode we iterate, to see if any resources were added
        //not for .class files though, adding them causes a restart
        //this is very important for bytebuddy performance
        boolean endsWithTrailingSlash = unsanitisedName.endsWith("/");
        if (name.endsWith(".class") && !endsWithTrailingSlash) {
            ClassPathElement classPathElement = classPathResourceIndex.getFirstClassPathElement(name);
            if (classPathElement != null) {
                final ClassPathResource resource = classPathElement.getResource(name);
                if (resource == null) {
                    throw new IllegalStateException(
                            classPathElement + " from " + getName() + " (closed=" + this.isClosed()
                                    + ") was expected to provide " + name + " but failed");
                }
                return resource.getUrl();
            }
        } else {
            URL url = getClassPathElementResourceUrl(normalPriorityElements, name, endsWithTrailingSlash);
            if (url != null) {
                return url;
            }
            url = getClassPathElementResourceUrl(lesserPriorityElements, name, endsWithTrailingSlash);
            if (url != null) {
                return url;
            }
        }
        return parent.getResource(unsanitisedName);
    }

    private static URL getClassPathElementResourceUrl(List<ClassPathElement> classPathElements, String name,
            boolean endsWithTrailingSlash) {
        for (int i = 0; i < classPathElements.size(); i++) {
            ClassPathResource res = classPathElements.get(i).getResource(name);
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

        return null;
    }

    @Override
    public InputStream getResourceAsStream(String unsanitisedName) {
        ensureOpen(unsanitisedName);

        for (int i = 0; i < classLoaderEventListeners.size(); i++) {
            classLoaderEventListeners.get(i).openResourceStream(unsanitisedName, this.name);
        }
        String name = sanitizeName(unsanitisedName);
        ClassPathResourceIndex classPathResourceIndex = getClassPathResourceIndex();
        if (classPathResourceIndex.isBanned(name)) {
            return null;
        }
        //dev mode may have added some files, so we iterate to check, but not for classes
        if (name.endsWith(".class")) {
            ClassPathElement classPathElement = classPathResourceIndex.getFirstClassPathElement(name);
            if (classPathElement != null) {
                final ClassPathResource resource = classPathElement.getResource(name);
                if (resource == null) {
                    throw new IllegalStateException(
                            classPathElement + " from " + getName() + " (closed=" + this.isClosed()
                                    + ") was expected to provide " + name + " but failed");
                }
                return new ByteArrayInputStream(resource.getData());
            }
        } else {
            InputStream inputStream = getClassPathElementResourceInputStream(normalPriorityElements, name);
            if (inputStream != null) {
                return inputStream;
            }
            inputStream = getClassPathElementResourceInputStream(lesserPriorityElements, name);
            if (inputStream != null) {
                return inputStream;
            }
        }
        return parent.getResourceAsStream(unsanitisedName);
    }

    private static InputStream getClassPathElementResourceInputStream(List<ClassPathElement> classPathElements, String name) {
        for (ClassPathElement classPathElement : classPathElements) {
            ClassPathResource res = classPathElement.getResource(name);
            if (res != null) {
                if (res.isDirectory()) {
                    try {
                        return res.getUrl().openStream();
                    } catch (IOException e) {
                        log.debug("Ignoring exception that occurred while opening a stream for resource " + name,
                                e);
                        // behave like how java.lang.ClassLoader#getResourceAsStream() behaves
                        // and don't propagate the exception
                        continue;
                    }
                }
                return new ByteArrayInputStream(res.getData());
            }
        }

        return null;
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
        ensureOpen(moduleName);

        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    protected URL findResource(String name) {
        ensureOpen(name);

        return getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        ensureOpen(name);

        return getResources(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        ensureOpen(name);

        return loadClass(name, false);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        ensureOpen(name);

        for (ClassLoaderEventListener l : classLoaderEventListeners) {
            l.loadClass(name, this.name);
        }
        if (isInJdkPackage(name)) {
            return parent.loadClass(name);
        }

        //even if the thread is interrupted we still want to be able to load classes
        //if the interrupt bit is set then we clear it and restore it at the end
        boolean interrupted = Thread.interrupted();
        try {
            ClassPathResourceIndex classPathResourceIndex = getClassPathResourceIndex();
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c != null) {
                    return c;
                }
                String resourceName = fromClassNameToResourceName(name);
                if (classPathResourceIndex.isBanned(resourceName)) {
                    throw new ClassNotFoundException(name);
                }
                boolean parentFirst = parentFirst(resourceName, classPathResourceIndex);
                if (parentFirst) {
                    try {
                        return parent.loadClass(name);
                    } catch (ClassNotFoundException ignore) {
                        log.tracef("Class %s not found in parent first load from %s", name, parent);
                    }
                }
                ClassPathElement classPathElement = classPathResourceIndex.getFirstClassPathElement(resourceName);
                if (classPathElement != null) {
                    final ClassPathResource classPathElementResource = classPathElement.getResource(resourceName);
                    if (classPathElementResource != null) { //can happen if the class loader was closed
                        byte[] data = classPathElementResource.getData();
                        definePackage(name, classPathElement);
                        Class<?> cl = defineClass(name, data, 0, data.length,
                                protectionDomains.computeIfAbsent(classPathElement,
                                        ClassPathElement::getProtectionDomain));
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

    @VisibleForTesting
    void definePackage(String name, ClassPathElement classPathElement) {
        var pkgName = getPackageNameFromClassName(name);
        if (pkgName == null) {
            return;
        }
        if (getDefinedPackage(pkgName) != null) {
            return;
        }
        try {
            var manifest = classPathElement.getManifestAttributes();
            if (manifest != null) {
                definePackage(pkgName, manifest.getSpecificationTitle(),
                        manifest.getSpecificationVersion(),
                        manifest.getSpecificationVendor(),
                        manifest.getImplementationTitle(),
                        manifest.getImplementationVersion(),
                        manifest.getImplementationVendor(), null);
            } else {
                definePackage(pkgName, null, null, null, null, null, null, null);
            }
        } catch (IllegalArgumentException e) {
            // retry, thrown by definePackage(), if a package for the same name is already defines by this class loader.
            if (getDefinedPackage(pkgName) != null) {
                return;
            }
            throw e;
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
        ensureOpen(name);

        return getElementsWithResource(name, false);
    }

    public List<ClassPathElement> getElementsWithResource(String name, boolean localOnly) {
        ensureOpen(name);

        final boolean parentFirst = parentFirst(name, getClassPathResourceIndex());

        List<ClassPathElement> result = List.of();

        if (parentFirst && !localOnly && parent instanceof QuarkusClassLoader parentQcl) {
            result = parentQcl.getElementsWithResource(name);
        }

        result = joinAndDedupe(result, getClassPathResourceIndex().getClassPathElements(name));

        if (!parentFirst && !localOnly && parent instanceof QuarkusClassLoader parentQcl) {
            result = joinAndDedupe(result, parentQcl.getElementsWithResource(name));
        }

        return result;
    }

    /**
     * Returns a list containing elements from two lists eliminating duplicates. Elements from the first list
     * will appear in the result before elements from the second list.
     * <p>
     * The current implementation assumes that none of the lists contains duplicates on their own but some elements
     * may be present in both lists.
     *
     * @param list1 first list
     * @param list2 second list
     * @return resulting list
     */
    private static <T> List<T> joinAndDedupe(List<T> list1, List<T> list2) {
        // it appears, in the vast majority of cases at least one of the lists will be empty
        if (list1.isEmpty()) {
            return list2;
        }
        if (list2.isEmpty()) {
            return list1;
        }
        final List<T> result = new ArrayList<>(list1.size() + list2.size());
        // it looks like in most cases at this point list1 (representing elements from the parent cl) will contain only one element
        if (list1.size() == 1) {
            final T firstCpe = list1.get(0);
            result.add(firstCpe);
            for (var cpe : list2) {
                if (cpe != firstCpe) {
                    result.add(cpe);
                }
            }
            return result;
        }
        result.addAll(list1);
        for (var cpe : list2) {
            if (!containsReference(list1, cpe)) {
                result.add(cpe);
            }
        }
        return result;
    }

    /**
     * Checks whether a list contains an element that references the other argument.
     *
     * @param list list of elements
     * @param e element to look for
     * @return true if the list contains an element referencing {@code e}, otherwise - false
     */
    private static <T> boolean containsReference(List<T> list, T e) {
        for (int i = list.size() - 1; i >= 0; --i) {
            if (e == list.get(i)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getReloadableClassNames() {
        ensureOpen();

        Set<String> ret = new HashSet<>();
        for (String resourceName : getClassPathResourceIndex().getReloadableClasses()) {
            ret.add(ClassLoaderHelper.fromResourceNameToClassName(resourceName));
        }
        return ret;
    }

    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        ensureOpen(name);

        return super.defineClass(name, b, off, len);
    }

    public void addCloseTask(Runnable task) {
        ensureOpen();

        synchronized (closeTasks) {
            closeTasks.add(task);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (status < STATUS_OPEN) {
                return;
            }
            status = STATUS_CLOSING;
        }

        if (lifecycleLog.isDebugEnabled()) {
            lifecycleLog.debugf(new RuntimeException("Created to log a stacktrace"), "Closing class loader %s", this);
        }

        List<Runnable> tasks;
        synchronized (closeTasks) {
            tasks = new ArrayList<>(closeTasks);
        }
        for (Runnable i : tasks) {
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
                byte[] data = is.readAllBytes();
                Runnable r = (Runnable) defineClass(DriverRemover.class.getName(), data, 0, data.length)
                        .getConstructor(ClassLoader.class).newInstance(this);
                r.run();
            } catch (Exception e) {
                log.debug("Failed to clean up DB drivers");
            }
        }

        closeClassPathElements(normalPriorityElements);
        // parentFirstElements are part of elements so no need to close them
        closeClassPathElements(lesserPriorityElements);
        closeClassPathElements(bannedElements);

        ResourceBundle.clearCache(this);

        status = STATUS_CLOSED;
    }

    private static void closeClassPathElements(List<ClassPathElement> classPathElements) {
        for (ClassPathElement element : classPathElements) {
            //note that this is a 'soft' close
            //all resources are closed, however the CL can still be used
            //but after close no resources will be held past the scope of an operation
            try (ClassPathElement ignored = element) {
                //the close() operation is implied by the try-with syntax
            } catch (Exception e) {
                log.error("Failed to close " + element, e);
            }
        }
    }

    public boolean isClosed() {
        return status < STATUS_OPEN;
    }

    private void ensureOpen(String name) {
        if (LOG_ACCESS_TO_CLOSED_CLASS_LOADERS && status == STATUS_CLOSED) {
            // we do not use a logger as it might require some class loading
            System.out.println("Class loader " + this + " has been closed and may not be accessed anymore. Attempted to load '"
                    + name + "'");
            Thread.dumpStack();
        }
    }

    private void ensureOpen() {
        if (LOG_ACCESS_TO_CLOSED_CLASS_LOADERS && status == STATUS_CLOSED) {
            // we do not use a logger as it might require some class loading
            System.out.println("Class loader " + this + " has been closed and may not be accessed anymore");
            Thread.dumpStack();
        }
    }

    public CuratedApplication getCuratedApplication() {
        return curatedApplication;
    }

    @Override
    public String toString() {
        return "QuarkusClassLoader:" + name + "@" + Integer.toHexString(hashCode());
    }

    public StartupAction getStartupAction() {
        return startupAction;
    }

    public void setStartupAction(StartupAction startupAction) {
        this.startupAction = startupAction;
    }

    public static class Builder {
        final String name;
        final ClassLoader parent;
        final List<ClassPathElement> normalPriorityElements = new ArrayList<>();
        final List<ClassPathElement> bannedElements = new ArrayList<>();
        final List<ClassPathElement> parentFirstElements = new ArrayList<>();
        final List<ClassPathElement> lesserPriorityElements = new ArrayList<>();
        final boolean parentFirst;
        CuratedApplication curatedApplication;
        MemoryClassPathElement resettableElement;
        private Map<String, byte[]> transformedClasses = Collections.emptyMap();
        boolean aggregateParentResources;
        boolean assertionsEnabled;
        private final ArrayList<ClassLoaderEventListener> classLoaderEventListeners = new ArrayList<>(5);

        public Builder(String name, ClassLoader parent, boolean parentFirst) {
            this.name = name;
            this.parent = parent;
            this.parentFirst = parentFirst;
        }

        /**
         * Adds an element that can be used to load classes.
         * <p>
         * Order is important, if there are multiple elements that provide the same
         * class then the first one passed to this method will be used.
         * <p>
         * The provided element will be closed when the ClassLoader is closed.
         *
         * @param element The element to add
         * @return This builder
         */
        public Builder addNormalPriorityElement(ClassPathElement element) {
            log.debugf("Adding normal priority element %s to QuarkusClassLoader %s", element, name);
            normalPriorityElements.add(element);
            return this;
        }

        /**
         * Adds an element which will only be used to load a class or resource if no normal priority
         * element containing that class or resource exists.
         * This is used in order control the order of elements when multiple contain the same classes
         *
         * @param element The element to add
         * @return This builder
         */
        public Builder addLesserPriorityElement(ClassPathElement element) {
            log.debugf("Adding lesser priority element %s to QuarkusClassLoader %s", element, name);
            lesserPriorityElements.add(element);
            return this;
        }

        /**
         * Adds a resettable MemoryClassPathElement to the class loader.
         * <p>
         * This element is mutable, and its contents will be modified if the class loader
         * is reset.
         * <p>
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
         * <p>
         * Note that this does not mean that the parent will always have this class, it is possible that
         * in some cases the class will end up being loaded by this loader, however an attempt will always
         * be made to load these from the parent CL first
         * <p>
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
         * <p>
         * Note that elements passed to this method will not be automatically closed, as
         * references to this element are not retained after the class loader has been built.
         * <p>
         * This is because banned elements are generally expected to be loaded by another ClassLoader,
         * so this prevents the need to create multiple ClassPathElements for the same resource.
         * <p>
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
         * If this is true then a getResources call will always include the parent resources.
         * <p>
         * If this is false then getResources will not return parent resources if local resources were found.
         * <p>
         * This only takes effect if parentFirst is false.
         */
        public Builder setAggregateParentResources(boolean aggregateParentResources) {
            this.aggregateParentResources = aggregateParentResources;
            return this;
        }

        public Builder setAssertionsEnabled(boolean assertionsEnabled) {
            this.assertionsEnabled = assertionsEnabled;
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

        public Builder setCuratedApplication(CuratedApplication curatedApplication) {
            this.curatedApplication = curatedApplication;
            return this;
        }

        /**
         * Builds the class loader
         *
         * @return The class loader
         */
        public QuarkusClassLoader build() {
            if (resettableElement != null) {
                if (!normalPriorityElements.contains(resettableElement)) {
                    normalPriorityElements.add(0, resettableElement);
                }
            }
            this.classLoaderEventListeners.trimToSize();
            return new QuarkusClassLoader(this);
        }

        @Override
        public String toString() {
            return "QuarkusClassLoader.Builder:" + name + "@" + Integer.toHexString(hashCode());
        }
    }

    public ClassLoader parent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

}
