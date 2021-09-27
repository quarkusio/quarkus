package io.quarkus.bootstrap.runner;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classloader used with the fast-jar package type.
 *
 * This ClassLoader takes advantage of the fact that Quarkus knows the entire classpath when the application is built,
 * and thus can index the location of classes and resources (with the result being written during the build to a binary file and
 * read at application startup).
 * The advantage this has over the JDK's default System ClassLoader is that it knows which jars contain the requested
 * classes and resources and thus does not have to iterate over all the jars on the classpath
 * (which is slow and takes up heap space as the jars need to be kept open), but instead can go directly to the
 * jar(s) containing the requested class or resource.
 * The implementation also contains optimizations that allow the ClassLoader to keep a minimum number of jars open
 * while also preventing the lookup of the entire classpath for missing resources in known directories (like META-INF/services).
 */
public final class RunnerClassLoader extends ClassLoader {

    /**
     * A map of resources by dir name. Root dir/default package is represented by the empty string
     */
    private final Map<String, ClassLoadingResource[]> resourceDirectoryMap;

    private final Set<String> parentFirstPackages;
    private final Set<String> nonExistentResources;
    // the following two fields go hand in hand - they need to both be populated from the same data
    // in order for the resource loading to work properly
    private final Set<String> fullyIndexedDirectories;
    private final Map<String, ClassLoadingResource[]> directlyIndexedResourcesIndexMap;

    //Mutations protected by synchronization on the field value itself:
    private final ClassLoadingResource[] currentlyBufferedResources = new ClassLoadingResource[4];//Experimentally found to be a reasonable number
    //Protected by synchronization on the above field, as they are related.
    private boolean postBootPhase = false;

    RunnerClassLoader(ClassLoader parent, Map<String, ClassLoadingResource[]> resourceDirectoryMap,
            Set<String> parentFirstPackages, Set<String> nonExistentResources,
            Set<String> fullyIndexedDirectories, Map<String, ClassLoadingResource[]> directlyIndexedResourcesIndexMap) {
        super(parent);
        this.resourceDirectoryMap = resourceDirectoryMap;
        this.parentFirstPackages = parentFirstPackages;
        this.nonExistentResources = nonExistentResources;
        this.fullyIndexedDirectories = fullyIndexedDirectories;
        this.directlyIndexedResourcesIndexMap = directlyIndexedResourcesIndexMap;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        //note that for performance reasons this CL does not do parent first delegation
        //although the intention is not for it to be a true isolated parent first CL
        //'delegation misses' where the parent throws a ClassNotFoundException are very expensive
        if (name.startsWith("java.")) {
            return getParent().loadClass(name);
        }
        String packageName = getPackageNameFromClassName(name);
        if (parentFirstPackages.contains(packageName)) {
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                //fall through
            }
        }
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        final ClassLoadingResource[] resources;
        if (packageName == null) {
            resources = resourceDirectoryMap.get("");
        } else {
            String dirName = packageName.replace('.', '/');
            resources = resourceDirectoryMap.get(dirName);
        }
        if (resources != null) {
            String classResource = name.replace('.', '/') + ".class";
            for (ClassLoadingResource resource : resources) {
                accessingResource(resource);
                byte[] data = resource.getResourceData(classResource);
                if (data == null) {
                    continue;
                }
                definePackage(packageName, resources);
                try {
                    return defineClass(name, data, 0, data.length, resource.getProtectionDomain());
                } catch (LinkageError e) {
                    loaded = findLoadedClass(name);
                    if (loaded != null) {
                        return loaded;
                    }
                    throw e;
                }
            }
        }
        return getParent().loadClass(name);
    }

    private void accessingResource(final ClassLoadingResource resource) {
        final ClassLoadingResource toEvict;
        synchronized (this.currentlyBufferedResources) {
            if (!postBootPhase) {
                //We only want to limit the jar buffers after the initial bootstrap has been completed
                return;
            }
            // This is not a cache aiming to accurately retain the most hot resources:
            // it's too small to benefit from traditional hit metrics,
            // we rather prefer to keep it very light.
            if (currentlyBufferedResources[0] == resource) {
                //it's already on the head of the cache: nothing to be done.
                return;
            }
            for (int i = 1; i < currentlyBufferedResources.length; i++) {
                final ClassLoadingResource currentI = currentlyBufferedResources[i];
                if (currentI == resource || currentI == null) {
                    //it was already cached, or we found an empty slot: bubble it up by one position to give it a boost
                    final ClassLoadingResource previous = currentlyBufferedResources[i - 1];
                    currentlyBufferedResources[i - 1] = resource;
                    currentlyBufferedResources[i] = previous;
                    return;
                }
            }
            // else, we drop one element from the cache,
            // and inserting the latest resource on the tail:
            toEvict = currentlyBufferedResources[currentlyBufferedResources.length - 1];
            currentlyBufferedResources[currentlyBufferedResources.length - 1] = resource;
        }
        // Finally, release the cache for the dropped element:
        toEvict.resetInternalCaches();
    }

    @Override
    protected URL findResource(String name) {
        name = sanitizeName(name);
        if (nonExistentResources.contains(name)) {
            return null;
        }
        ClassLoadingResource[] resources = getClassLoadingResources(name);
        if (resources == null)
            return null;
        for (ClassLoadingResource resource : resources) {
            accessingResource(resource);
            URL data = resource.getResourceURL(name);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private String sanitizeName(final String name) {
        if (name.length() > 0 && name.charAt(0) == '/') {
            return name.substring(1);
        }
        return name;
    }

    private ClassLoadingResource[] getClassLoadingResources(final String name) {
        ClassLoadingResource[] resources = directlyIndexedResourcesIndexMap.get(name);
        if (resources != null) {
            return resources;
        }
        String dirName = getDirNameFromResourceName(name);
        if (dirName == null) {
            dirName = "";
        }
        if (!dirName.equals(name) && fullyIndexedDirectories.contains(dirName)) {
            // If we arrive here, we know that resource being queried belongs to one of the fully indexed directories
            // Had that resource existed however, it would have been present in directlyIndexedResourcesIndexMap
            return null;
        }
        resources = resourceDirectoryMap.get(dirName);
        if (resources == null) {
            // the resource could itself be a directory
            resources = resourceDirectoryMap.get(name);
        }
        return resources;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        name = sanitizeName(name);
        if (nonExistentResources.contains(name)) {
            return Collections.emptyEnumeration();
        }
        ClassLoadingResource[] resources = getClassLoadingResources(name);
        if (resources == null)
            return Collections.emptyEnumeration();
        List<URL> urls = new ArrayList<>();
        for (ClassLoadingResource resource : resources) {
            accessingResource(resource);
            URL data = resource.getResourceURL(name);
            if (data != null) {
                urls.add(data);
            }
        }
        return Collections.enumeration(urls);
    }

    private void definePackage(String pkgName, ClassLoadingResource[] resources) {
        if ((pkgName != null) && getPackage(pkgName) == null) {
            synchronized (getClassLoadingLock(pkgName)) {
                if (getPackage(pkgName) == null) {
                    for (ClassLoadingResource classPathElement : resources) {
                        ManifestInfo mf = classPathElement.getManifestInfo();
                        if (mf != null) {
                            definePackage(pkgName, mf.getSpecTitle(),
                                    mf.getSpecVersion(),
                                    mf.getSpecVendor(),
                                    mf.getImplTitle(),
                                    mf.getImplVersion(),
                                    mf.getImplVendor(), null);
                            return;
                        }
                    }
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

    private String getDirNameFromResourceName(String resourceName) {
        final int index = resourceName.lastIndexOf('/');
        if (index == -1) {
            // we return null here since in this case no package is defined
            // this is same behavior as Package.getPackage(clazz) exhibits
            // when the class is in the default package
            return null;
        }
        return resourceName.substring(0, index);
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

    public void close() {
        for (Map.Entry<String, ClassLoadingResource[]> entry : resourceDirectoryMap.entrySet()) {
            for (ClassLoadingResource i : entry.getValue()) {
                i.close();
            }
        }
    }

    public void resetInternalCaches() {
        synchronized (this.currentlyBufferedResources) {
            for (Map.Entry<String, ClassLoadingResource[]> entry : resourceDirectoryMap.entrySet()) {
                for (ClassLoadingResource i : entry.getValue()) {
                    i.resetInternalCaches();
                }
            }
            this.postBootPhase = true;
        }
    }
}
